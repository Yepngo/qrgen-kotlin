package com.yepngo.qrgen.renderers.common

import com.yepngo.qrgen.utils.ColorConfig
import java.awt.Color

internal class ImgParameters(
    val cellSize: Int,
    val matrixWidthInCells: Int,
    val firstCellX: Int,
    val firstCellY: Int,
    private val colorConfig: ColorConfig,
) {
    val onColorForAwt: Color
        get() = colorConfig.onColorForAwt

    val offColorForAwt: Color
        get() = colorConfig.offColorForAwt

    val outerMarkerColorForAwt: Color
        get() = colorConfig.outerMarkerColorForAwt

    val innerMarkerColorForAwt: Color
        get() = colorConfig.innerMarkerColorForAwt
}
