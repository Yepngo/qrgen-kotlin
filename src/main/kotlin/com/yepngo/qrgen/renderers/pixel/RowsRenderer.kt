package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class RowsRenderer(
    imgParams: ImgParameters,
) : PixelRenderer(imgParams) {
    protected override fun renderActiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        val hasLeft = context.isNeighbourSet(PixelContext.Direction.W)
        val hasRight = context.isNeighbourSet(PixelContext.Direction.E)
        val cellSize: Int = imgParams.cellSize
        val yStart = cellSize / 6
        val yEnd = cellSize - yStart

        if (hasLeft && hasRight) {
            gfx.fillRect(0, yStart, cellSize, yEnd)
        } else {
            if (hasLeft) {
                gfx.fillRect(0, yStart, cellSize / 2, yEnd)
            }

            if (hasRight) {
                gfx.fillRect(cellSize / 2, yStart, cellSize, yEnd)
            }

            gfx.fillOval(yStart, yStart, yEnd, yEnd)
        }
    }
}
