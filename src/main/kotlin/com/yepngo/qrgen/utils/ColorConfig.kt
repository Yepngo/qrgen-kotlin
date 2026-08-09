package com.yepngo.qrgen.utils

import com.yepngo.qrgen.colors.QrColor
import java.awt.Color
import java.awt.image.BufferedImage

internal data class ColorConfig(
    val pixels: QrColor,
    val background: QrColor,
    val outerMarker: QrColor,
    val innerMarker: QrColor,
) {
    val onColorForAwt: Color get() = pixels.asAwtColor()
    val offColorForAwt: Color get() = background.asAwtColor()
    val outerMarkerColorForAwt: Color get() = outerMarker.asAwtColor()
    val innerMarkerColorForAwt: Color get() = innerMarker.asAwtColor()

    fun determineImageType(): Int =
        if (
            pixels.hasAlpha() || background.hasAlpha() || outerMarker.hasAlpha() || innerMarker.hasAlpha()
        ) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            BufferedImage.TYPE_INT_RGB
        }
}
