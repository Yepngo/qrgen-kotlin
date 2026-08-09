package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import com.yepngo.qrgen.renderers.common.SvgPath2D.Companion.drawSvgCommand
import java.awt.Graphics2D

internal abstract class PixelRenderer(
    private val imgParams: ImgParameters,
) {
    fun renderPixel(
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        if (context.isSet) {
            renderActiveShape(imgParams, context, gfx)
        } else {
            renderInactiveShape(imgParams, context, gfx)
        }
    }

    protected open fun renderActiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
    }

    protected open fun renderInactiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
    }

    protected fun renderPixelFromSvgPath(
        imgParams: ImgParameters,
        gfx: Graphics2D,
        path: String,
    ) {
        val cellSize = imgParams.cellSize.toDouble()
        val factor = cellSize / 140.0

        val transform = gfx.getTransform()
        gfx.scale(factor, factor)
        gfx.fill(drawSvgCommand(path))
        gfx.setTransform(transform)
    }
}
