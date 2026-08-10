package com.yepngo.qrgen

import com.yepngo.qrgen.colors.QrColor
import com.yepngo.qrgen.config.ErrorCorrectionLevel
import com.yepngo.qrgen.config.ImageFileType
import com.yepngo.qrgen.config.MarkerStyle
import com.yepngo.qrgen.config.PixelStyle
import com.yepngo.qrgen.internal.zxing.EncodeHintType
import com.yepngo.qrgen.renderers.QrCodeRenderer
import com.yepngo.qrgen.utils.ColorConfig
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.max

/** Marks receivers belonging to the QR generator DSL. */
@DslMarker
public annotation class QrGeneratorDsl

/** Creates an immutable QR generator from a Kotlin DSL configuration. */
public fun qrGenerator(block: QrGeneratorBuilder.() -> Unit = {}): QrGenerator = QrGeneratorBuilder().apply(block).build()

/** Mutable construction-time receiver for an immutable [QrGenerator]. */
@QrGeneratorDsl
public class QrGeneratorBuilder public constructor() {
    /** Output image format. */
    public var imageType: ImageFileType = ImageFileType.PNG

    /** Character encoding used for the payload. */
    public var charset: Charset = StandardCharsets.UTF_8

    /** QR error-correction level. */
    public var errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.L

    /** Shape used for normal QR modules. */
    public var pixelStyle: PixelStyle = PixelStyle.RECTANGLES

    /** Shape used for the three position markers. */
    public var markerStyle: MarkerStyle = MarkerStyle.RECTANGLES

    private var width: Int = DEFAULT_SIZE
    private var height: Int = DEFAULT_SIZE
    private var margin: Int = DEFAULT_MARGIN
    private var colorConfig: ColorConfig = DEFAULT_COLORS
    private var logoImage: BufferedImage? = null

    /** Sets the output dimensions in pixels. */
    public fun size(
        width: Int,
        height: Int,
    ) {
        require(width > 0 && height > 0) { "width and height must be positive" }
        this.width = width
        this.height = height
    }

    /** Sets the quiet-zone margin in QR cells. */
    public fun margin(cells: Int) {
        require(cells >= 0) { "margin must be non-negative" }
        margin = cells
    }

    /** Configures the four rendering colors. */
    public fun colors(block: QrColorsBuilder.() -> Unit) {
        colorConfig = QrColorsBuilder(colorConfig).apply(block).build()
    }

    /** Decodes and copies a logo from [path] immediately. */
    public fun logo(path: Path) {
        try {
            Files.newInputStream(path).use(::logo)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Unable to read logo from $path", exception)
        }
    }

    /** Decodes and copies a logo without closing the caller-owned [input]. */
    public fun logo(input: InputStream) {
        val decoded =
            try {
                ImageIO.read(input)
            } catch (exception: Exception) {
                throw IllegalArgumentException("Unable to decode logo", exception)
            } ?: throw IllegalArgumentException("Unable to decode logo")
        logoImage = decoded.copyImage()
    }

    /** Defensively copies [image] immediately. */
    public fun logo(image: BufferedImage) {
        require(image.width > 0 && image.height > 0) { "logo must not be empty" }
        logoImage = image.copyImage()
    }

    /** Builds an immutable, reusable generator. */
    public fun build(): QrGenerator =
        QrGenerator(
            GeneratorConfig(
                imageType = imageType,
                width = width,
                height = height,
                margin = margin,
                charset = charset,
                errorCorrection = errorCorrection,
                pixelStyle = pixelStyle,
                markerStyle = markerStyle,
                colors = colorConfig,
                logo = logoImage?.copyImage(),
            ),
        )

    private companion object {
        const val DEFAULT_SIZE: Int = 200
        const val DEFAULT_MARGIN: Int = 4
        val DEFAULT_COLORS: ColorConfig =
            ColorConfig(
                pixels = QrColor.rgb(0, 0, 0),
                background = QrColor.rgb(255, 255, 255),
                outerMarker = QrColor.rgb(0, 0, 0),
                innerMarker = QrColor.rgb(0, 0, 0),
            )
    }
}

/** Nested receiver for QR rendering colors. */
@QrGeneratorDsl
public class QrColorsBuilder internal constructor(
    config: ColorConfig,
) {
    /** Color of normal QR modules. */
    public var pixels: QrColor = config.pixels

    /** Canvas background color. */
    public var background: QrColor = config.background

    /** Color of the outer position-marker structures. */
    public var outerMarker: QrColor = config.outerMarker

    /** Color of the inner position-marker structures. */
    public var innerMarker: QrColor = config.innerMarker

    internal fun build(): ColorConfig = ColorConfig(pixels, background, outerMarker, innerMarker)
}

/** Immutable and thread-safe configured QR generator. */
public class QrGenerator internal constructor(
    private val config: GeneratorConfig,
) {
    /** Renders [payload] into a new image. */
    public fun render(payload: String): Result<BufferedImage> =
        runCatching {
            require(payload.toByteArray(config.charset).size <= config.errorCorrection.maximumPayloadBytes) {
                "payload exceeds ${config.errorCorrection} capacity of ${config.errorCorrection.maximumPayloadBytes} bytes"
            }
            val hints: MutableMap<EncodeHintType?, Any> =
                hashMapOf(
                    EncodeHintType.CHARACTER_SET to config.charset.name(),
                    EncodeHintType.ERROR_CORRECTION to
                        com.yepngo.qrgen.internal.zxing.qrcode.decoder.ErrorCorrectionLevel.valueOf(
                            config.errorCorrection.name,
                        ),
                    EncodeHintType.MARGIN to config.margin,
                )
            val image =
                QrCodeRenderer(config.pixelStyle, config.markerStyle).encodeAndRender(
                    payload,
                    config.colors,
                    config.width,
                    config.height,
                    hints,
                )
            config.logo?.let { image.withLogo(it) } ?: image
        }

    /** Encodes [payload] directly to caller-owned [output]. */
    public fun write(
        payload: String,
        output: OutputStream,
    ): Result<Unit> =
        render(payload).mapCatching { image ->
            check(ImageIO.write(image, config.imageType.formatName, output)) {
                "No ImageIO writer available for ${config.imageType.formatName}"
            }
        }

    /** Writes [payload] to a newly created temporary file. */
    public fun writeTemp(
        payload: String,
        prefix: String? = null,
    ): Result<Path> =
        runCatching {
            val file = Files.createTempFile(prefix ?: "qrgen-", ".${config.imageType.extension}")
            Files.newOutputStream(file).use { output -> write(payload, output).getOrThrow() }
            file
        }
}

internal data class GeneratorConfig(
    val imageType: ImageFileType,
    val width: Int,
    val height: Int,
    val margin: Int,
    val charset: Charset,
    val errorCorrection: ErrorCorrectionLevel,
    val pixelStyle: PixelStyle,
    val markerStyle: MarkerStyle,
    val colors: ColorConfig,
    val logo: BufferedImage?,
)

private val ImageFileType.formatName: String
    get() = if (this == ImageFileType.JPG) "JPEG" else name

private val ImageFileType.extension: String
    get() = if (this == ImageFileType.JPG) "jpg" else name.lowercase()

private fun BufferedImage.copyImage(): BufferedImage {
    val copy = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    copy.createGraphics().useGraphics { graphics -> graphics.drawImage(this, 0, 0, null) }
    return copy
}

private fun BufferedImage.withLogo(logo: BufferedImage): BufferedImage {
    val outputWidth = max(width, logo.width)
    val outputHeight = max(height, logo.height)
    val output = BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB)
    output.createGraphics().useGraphics { graphics ->
        graphics.composite = AlphaComposite.Src
        graphics.drawImage(this, (outputWidth - width) / 2, (outputHeight - height) / 2, null)
        graphics.composite = AlphaComposite.SrcOver
        graphics.drawImage(logo, (outputWidth - logo.width) / 2, (outputHeight - logo.height) / 2, null)
    }
    return output
}

private inline fun java.awt.Graphics2D.useGraphics(block: (java.awt.Graphics2D) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
