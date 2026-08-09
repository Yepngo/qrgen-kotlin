package com.yepngo.qrgen.colors

import java.awt.Color
import kotlin.math.roundToInt

/** An immutable color used while rendering a QR code. */
public class QrColor private constructor(
    private val value: Int,
) {
    /** The alpha component in the range 0 through 255. */
    public val alpha: Int
        get() = value ushr 24

    /** Whether this color contains transparency. */
    public fun hasAlpha(): Boolean = alpha != OPAQUE

    /** Returns this color with a fully opaque alpha component. */
    public fun withoutAlpha(): QrColor = if (hasAlpha()) QrColor(value or OPAQUE_MASK) else this

    /** Scales the red, green, and blue components while retaining alpha. */
    public fun scale(factor: Double): QrColor {
        require(factor >= 0.0 && factor.isFinite()) { "factor must be finite and non-negative" }
        return argb(alpha, scaleComponent(red, factor), scaleComponent(green, factor), scaleComponent(blue, factor))
    }

    /** Converts this value to its AWT representation. */
    public fun asAwtColor(): Color = Color(value, true)

    private val red: Int get() = value ushr 16 and 0xff
    private val green: Int get() = value ushr 8 and 0xff
    private val blue: Int get() = value and 0xff

    public override fun equals(other: Any?): Boolean = other is QrColor && value == other.value

    public override fun hashCode(): Int = value

    public override fun toString(): String = "QrColor(argb=0x${value.toUInt().toString(16).padStart(8, '0')})"

    public companion object {
        private const val OPAQUE: Int = 255
        private const val OPAQUE_MASK: Int = -0x1000000

        /** Creates an opaque color from individual RGB components. */
        public fun rgb(
            red: Int,
            green: Int,
            blue: Int,
        ): QrColor = argb(OPAQUE, red, green, blue)

        /** Creates an opaque color from a packed `0xRRGGBB` value. */
        public fun rgb(rgb: Int): QrColor {
            require(rgb and OPAQUE_MASK == 0) { "rgb must be in the range 0x000000 through 0xFFFFFF" }
            return QrColor(OPAQUE_MASK or rgb)
        }

        /** Creates a color from RGBA components. */
        public fun rgba(
            red: Int,
            green: Int,
            blue: Int,
            alpha: Int,
        ): QrColor = argb(alpha, red, green, blue)

        /** Creates a color from a packed `0xRRGGBBAA` value. */
        public fun rgba(rgba: UInt): QrColor {
            val raw = rgba.toInt()
            return argb(raw and 0xff, raw ushr 24 and 0xff, raw ushr 16 and 0xff, raw ushr 8 and 0xff)
        }

        /** Creates a color from ARGB components. */
        public fun argb(
            alpha: Int,
            red: Int,
            green: Int,
            blue: Int,
        ): QrColor {
            validate("alpha", alpha)
            validate("red", red)
            validate("green", green)
            validate("blue", blue)
            return QrColor(alpha shl 24 or (red shl 16) or (green shl 8) or blue)
        }

        /** Creates a color from a packed `0xAARRGGBB` value. */
        public fun argb(argb: UInt): QrColor = QrColor(argb.toInt())

        /** Creates an opaque color from HSL components. */
        public fun hsl(
            hue: Double,
            saturation: Double,
            lightness: Double,
        ): QrColor = hsla(hue, saturation, lightness, OPAQUE)

        /** Creates a color from HSL components and an alpha byte. */
        public fun hsla(
            hue: Double,
            saturation: Double,
            lightness: Double,
            alpha: Int,
        ): QrColor {
            require(hue in 0.0..360.0) { "hue must be in the range 0 through 360" }
            require(saturation in 0.0..100.0) { "saturation must be in the range 0 through 100" }
            require(lightness in 0.0..100.0) { "lightness must be in the range 0 through 100" }
            validate("alpha", alpha)

            val h = if (hue == 360.0) 0.0 else hue / 360.0
            val s = saturation / 100.0
            val l = lightness / 100.0
            if (s == 0.0) {
                val gray = (l * 255.0).roundToInt()
                return argb(alpha, gray, gray, gray)
            }
            val q = if (l < 0.5) l * (1.0 + s) else l + s - l * s
            val p = 2.0 * l - q
            return argb(alpha, hueToRgb(p, q, h + 1.0 / 3.0), hueToRgb(p, q, h), hueToRgb(p, q, h - 1.0 / 3.0))
        }

        private fun hueToRgb(
            p: Double,
            q: Double,
            source: Double,
        ): Int {
            var t = source
            if (t < 0.0) t += 1.0
            if (t > 1.0) t -= 1.0
            val component =
                when {
                    t < 1.0 / 6.0 -> p + (q - p) * 6.0 * t
                    t < 1.0 / 2.0 -> q
                    t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6.0
                    else -> p
                }
            return (component * 255.0).roundToInt()
        }

        private fun scaleComponent(
            component: Int,
            factor: Double,
        ): Int = (component * factor).coerceAtMost(255.0).toInt()

        private fun validate(
            name: String,
            component: Int,
        ) {
            require(component in 0..255) { "$name must be in the range 0 through 255" }
        }
    }
}
