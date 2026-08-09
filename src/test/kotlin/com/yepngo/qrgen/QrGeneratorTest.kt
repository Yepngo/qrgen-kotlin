package com.yepngo.qrgen

import com.yepngo.qrgen.colors.QrColor
import com.yepngo.qrgen.config.ErrorCorrectionLevel
import com.yepngo.qrgen.config.ImageFileType
import com.yepngo.qrgen.config.MarkerStyle
import com.yepngo.qrgen.config.PixelStyle
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.stream.Stream
import javax.imageio.ImageIO

class QrGeneratorTest {
    @Test
    fun defaultsRenderPngAtTwoHundredPixels() {
        val image = qrGenerator().render("defaults").getOrThrow()
        assertEquals(200, image.width)
        assertEquals(200, image.height)
        assertEquals(Color.WHITE.rgb, image.getRGB(0, 0))
    }

    @ParameterizedTest
    @MethodSource("imageSizes")
    fun configuredSizeIsRespected(
        width: Int,
        height: Int,
    ) {
        val image = qrGenerator { size(width, height) }.render("size").getOrThrow()
        assertEquals(width, image.width)
        assertEquals(height, image.height)
    }

    @ParameterizedTest
    @EnumSource(ImageFileType::class)
    fun everyImageFormatCanBeWritten(type: ImageFileType) {
        val path = qrGenerator { imageType = type }.writeTemp("format-$type").getOrThrow()
        try {
            assertEquals(type, TestUtilities.findOutImageTypeOfFile(path))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @ParameterizedTest
    @EnumSource(PixelStyle::class)
    fun everyPixelStyleRenders(style: PixelStyle) {
        val image =
            qrGenerator {
                size(320, 320)
                pixelStyle = style
            }.render("pixel style $style").getOrThrow()
        assertEquals(320, image.width)
    }

    @ParameterizedTest
    @EnumSource(MarkerStyle::class)
    fun everyMarkerStyleRenders(style: MarkerStyle) {
        val image =
            qrGenerator {
                size(320, 320)
                markerStyle = style
            }.render("marker style $style").getOrThrow()
        assertEquals(320, image.height)
    }

    @ParameterizedTest
    @MethodSource("payloadLimits")
    fun maximumPayloadsRender(level: ErrorCorrectionLevel) {
        val payload = Payload.getWithLength(level.maximumPayloadBytes)
        assertTrue(qrGenerator { errorCorrection = level }.render(payload).isSuccess)
    }

    @ParameterizedTest
    @MethodSource("payloadLimits")
    fun oversizedPayloadsAreResultFailures(level: ErrorCorrectionLevel) {
        val payload = Payload.getWithLength(level.maximumPayloadBytes + 1)
        assertTrue(qrGenerator { errorCorrection = level }.render(payload).isFailure)
    }

    @Test
    fun writeUsesCallerOwnedOutputStreamDirectly() {
        val output = ByteArrayOutputStream()
        qrGenerator { size(300, 300) }.write("stream", output).getOrThrow()
        val image = ImageIO.read(ByteArrayInputStream(output.toByteArray()))
        assertEquals(300, image.width)
        assertEquals(300, image.height)
    }

    @Test
    fun outputFailureIsCapturedInResult() {
        val failing =
            object : OutputStream() {
                override fun write(value: Int) = throw IOException("expected")
            }
        assertTrue(qrGenerator().write("failure", failing).isFailure)
    }

    @Test
    fun renderingFailureIsCapturedInResult() {
        val generator = qrGenerator { size(1, 1) }
        assertTrue(generator.render("too small").isFailure)
    }

    @ParameterizedTest
    @MethodSource("invalidConfigurations")
    fun invalidConfigurationFailsImmediately(configuration: QrGeneratorBuilder.() -> Unit) {
        assertThrows(IllegalArgumentException::class.java) { qrGenerator(configuration) }
    }

    @Test
    fun colorsOverrideBackgroundAndSupportAlpha() {
        val image =
            qrGenerator {
                colors {
                    pixels = QrColor.rgb(0, 0, 255)
                    background = QrColor.rgba(255, 0, 0, 128)
                    outerMarker = QrColor.rgb(0, 255, 0)
                    innerMarker = QrColor.rgb(255, 255, 0)
                }
            }.render("colors").getOrThrow()
        assertEquals(Color(255, 0, 0, 128).rgb, image.getRGB(0, 0))
        assertTrue(image.colorModel.hasAlpha())
    }

    @Test
    fun bufferedImageLogoIsDefensivelyCopied() {
        val logo =
            BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().also { graphics ->
                    graphics.color = Color.MAGENTA
                    graphics.fillRect(0, 0, width, height)
                    graphics.dispose()
                }
            }
        val generator = qrGenerator { logo(logo) }
        logo.setRGB(0, 0, Color.GREEN.rgb)
        val rendered = generator.render("logo copy").getOrThrow()
        assertEquals(Color.MAGENTA.rgb, rendered.getRGB(rendered.width / 2 - 4, rendered.height / 2 - 4))
    }

    @Test
    fun inputStreamLogoIsDecodedWithoutClosingCallerStream() {
        val bytes = logoBytes()
        val input = TrackingInputStream(bytes)
        val generator = qrGenerator { logo(input) }
        assertFalse(input.closed)
        assertNotNull(generator.render("stream logo").getOrThrow())
    }

    @Test
    fun pathLogoIsDecodedDuringDslConstruction() {
        val path = Files.createTempFile("qrgen-logo-", ".png")
        Files.write(path, logoBytes())
        val generator =
            try {
                qrGenerator { logo(path) }
            } finally {
                Files.delete(path)
            }
        assertNotNull(generator.render("path logo").getOrThrow())
    }

    @Test
    fun invalidLogoFailsDuringDslConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            qrGenerator { logo(ByteArrayInputStream(byteArrayOf(1, 2, 3))) }
        }
    }

    @Test
    fun generatorIsReusable() {
        val generator = qrGenerator { size(240, 240) }
        val first = generator.render("first").getOrThrow()
        val second = generator.render("second").getOrThrow()
        assertEquals(first.width, second.width)
        assertFalse(first === second)
    }

    @Test
    fun generatorRendersConcurrently() {
        val generator = qrGenerator { size(250, 250) }
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results =
                executor.invokeAll(
                    (1..16).map { index ->
                        Callable { generator.render("concurrent-$index").getOrThrow() }
                    },
                )
            assertTrue(results.all { it.get().width == 250 })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun temporaryFileUsesRequestedPrefix() {
        val path = qrGenerator().writeTemp("temp", "custom-prefix-").getOrThrow()
        try {
            assertTrue(path.fileName.toString().startsWith("custom-prefix-"))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun noJavaSourcesRemain() {
        Files.walk(Path.of(System.getProperty("user.dir"), "src")).use { paths ->
            assertFalse(paths.anyMatch { it.fileName.toString().endsWith(".java") })
        }
    }

    private class TrackingInputStream(
        bytes: ByteArray,
    ) : ByteArrayInputStream(bytes) {
        var closed: Boolean = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    companion object {
        @JvmStatic
        fun imageSizes(): Stream<Arguments> =
            Stream.of(
                Arguments.of(100, 100),
                Arguments.of(300, 200),
                Arguments.of(650, 650),
                Arguments.of(900, 500),
            )

        @JvmStatic
        fun payloadLimits(): Stream<Arguments> = ErrorCorrectionLevel.entries.stream().map(Arguments::of)

        @JvmStatic
        fun invalidConfigurations(): Stream<Arguments> =
            Stream.of(
                Arguments.of({ builder: QrGeneratorBuilder -> builder.size(0, 100) }),
                Arguments.of({ builder: QrGeneratorBuilder -> builder.size(100, -1) }),
                Arguments.of({ builder: QrGeneratorBuilder -> builder.margin(-1) }),
            )

        private fun logoBytes(): ByteArray {
            val image = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
            image.createGraphics().also { graphics ->
                graphics.color = Color.MAGENTA
                graphics.fillRect(0, 0, image.width, image.height)
                graphics.dispose()
            }
            return ByteArrayOutputStream().also { ImageIO.write(image, "PNG", it) }.toByteArray()
        }
    }
}
