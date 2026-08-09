package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class CirclesRenderer(
    imgParams: ImgParameters,
) : IndependentPixelRenderer(imgParams) {
    override fun drawActualShape(
        imgParams: ImgParameters,
        gfx: Graphics2D,
    ) {
        gfx.fillOval(0, 0, imgParams.cellSize, imgParams.cellSize)
    }
}
