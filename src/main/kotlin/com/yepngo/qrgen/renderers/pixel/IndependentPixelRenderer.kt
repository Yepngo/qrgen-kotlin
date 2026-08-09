package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal abstract class IndependentPixelRenderer(
    imgParams: ImgParameters,
) : PixelRenderer(imgParams) {
    protected open val svgPath: String
        get() = // a rectangle which fills the complete cell
            "h 140 v 140 h -140 z"

    protected open fun drawActualShape(
        imgParams: ImgParameters,
        gfx: Graphics2D,
    ) {
        renderPixelFromSvgPath(imgParams, gfx, this.svgPath)
    }

    override fun renderActiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        drawActualShape(imgParams, gfx)
    }
}
