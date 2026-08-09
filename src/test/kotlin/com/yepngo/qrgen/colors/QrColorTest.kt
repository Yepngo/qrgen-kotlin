package com.yepngo.qrgen.colors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class QrColorTest {
    @Test
    fun rgbComponentsArePreserved() {
        assertEquals(java.awt.Color(0x12, 0x34, 0x56), QrColor.rgb(0x12, 0x34, 0x56).asAwtColor())
    }

    @Test
    fun packedRgbIsPreserved() {
        assertEquals(java.awt.Color(0x12, 0x34, 0x56), QrColor.rgb(0x123456).asAwtColor())
    }

    @Test
    fun rgbaComponentsArePreserved() {
        assertEquals(java.awt.Color(0x12, 0x34, 0x56, 0x78), QrColor.rgba(0x12, 0x34, 0x56, 0x78).asAwtColor())
    }

    @Test
    fun packedRgbaIsPreserved() {
        assertEquals(java.awt.Color(0x12, 0x34, 0x56, 0x78), QrColor.rgba(0x12345678u).asAwtColor())
    }

    @Test
    fun packedArgbIsPreserved() {
        assertEquals(java.awt.Color(0x34, 0x56, 0x78, 0x12), QrColor.argb(0x12345678u).asAwtColor())
    }

    @ParameterizedTest
    @MethodSource("hslConversions")
    fun hslFactoriesMatchExpectedColors(
        color: QrColor,
        expected: java.awt.Color,
    ) {
        assertEquals(expected, color.asAwtColor())
    }

    @ParameterizedTest
    @MethodSource("invalidComponents")
    fun componentBoundsAreValidated(factory: () -> QrColor) {
        assertThrows(IllegalArgumentException::class.java) { factory() }
    }

    @Test
    fun scaleRetainsAlphaAndClampsComponents() {
        assertEquals(java.awt.Color(60, 255, 180, 120), QrColor.rgba(30, 180, 90, 120).scale(2.0).asAwtColor())
    }

    @Test
    fun negativeScaleFailsImmediately() {
        assertThrows(IllegalArgumentException::class.java) { QrColor.rgb(1, 2, 3).scale(-1.0) }
    }

    @Test
    fun alphaCanBeInspectedAndRemoved() {
        val transparent = QrColor.rgba(10, 20, 30, 40)
        assertTrue(transparent.hasAlpha())
        assertEquals(40, transparent.alpha)
        assertEquals(java.awt.Color(10, 20, 30), transparent.withoutAlpha().asAwtColor())
    }

    @Test
    fun removingAbsentAlphaReturnsSameValue() {
        val opaque = QrColor.rgb(10, 20, 30)
        assertFalse(opaque.hasAlpha())
        assertSame(opaque, opaque.withoutAlpha())
    }

    companion object {
        @JvmStatic
        fun hslConversions(): Stream<Arguments> =
            Stream.of(
                Arguments.of(QrColor.hsl(0.0, 100.0, 50.0), java.awt.Color.RED),
                Arguments.of(QrColor.hsl(120.0, 100.0, 50.0), java.awt.Color.GREEN),
                Arguments.of(QrColor.hsl(240.0, 100.0, 50.0), java.awt.Color.BLUE),
                Arguments.of(QrColor.hsla(348.0, 53.0, 55.0, 128), java.awt.Color(0xc9, 0x4f, 0x68, 128)),
            )

        @JvmStatic
        fun invalidComponents(): Stream<Arguments> =
            Stream.of(
                Arguments.of({ QrColor.rgb(-1, 0, 0) }),
                Arguments.of({ QrColor.rgb(0, 256, 0) }),
                Arguments.of({ QrColor.rgba(0, 0, 0, -1) }),
                Arguments.of({ QrColor.argb(256, 0, 0, 0) }),
                Arguments.of({ QrColor.rgb(0x1000000) }),
                Arguments.of({ QrColor.hsl(-0.1, 50.0, 50.0) }),
                Arguments.of({ QrColor.hsl(360.1, 50.0, 50.0) }),
                Arguments.of({ QrColor.hsl(100.0, -1.0, 50.0) }),
                Arguments.of({ QrColor.hsl(100.0, 50.0, 101.0) }),
            )
    }
}
