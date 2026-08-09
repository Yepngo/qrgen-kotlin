package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class SnakesRenderer(
    imgParams: ImgParameters,
) : PixelRenderer(imgParams) {
    protected override fun renderActiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        val cellSize: Int = imgParams.cellSize

        gfx.fillOval(0, 0, cellSize, cellSize)

        if (context.isNeighbourSet(PixelContext.Direction.W)) {
            val path = "h 70 v 140 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (context.isNeighbourSet(PixelContext.Direction.N)) {
            val path = "h 140 v 70 h -140 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (context.isNeighbourSet(PixelContext.Direction.E)) {
            val path = "m 70,0 h 70 v 140 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (context.isNeighbourSet(PixelContext.Direction.S)) {
            val path = "m 0,70 h 140 v 70 h -140 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }
    }
}
