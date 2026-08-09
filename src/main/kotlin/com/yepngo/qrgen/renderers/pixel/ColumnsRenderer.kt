package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class ColumnsRenderer(
    imgParams: ImgParameters,
) : PixelRenderer(imgParams) {
    override fun renderActiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        val hasTop = context.isNeighbourSet(PixelContext.Direction.N)
        val hasBottom = context.isNeighbourSet(PixelContext.Direction.S)
        val cellSize = imgParams.cellSize
        val xStart = cellSize / 6
        val xEnd = cellSize - xStart

        if (hasTop && hasBottom) {
            gfx.fillRect(xStart, 0, xEnd, cellSize)
        } else {
            if (hasTop) {
                gfx.fillRect(xStart, 0, xEnd, cellSize / 2)
            }

            if (hasBottom) {
                gfx.fillRect(xStart, cellSize / 2, xEnd, cellSize)
            }

            gfx.fillOval(xStart, xStart, xEnd, xEnd)
        }
    }
}
