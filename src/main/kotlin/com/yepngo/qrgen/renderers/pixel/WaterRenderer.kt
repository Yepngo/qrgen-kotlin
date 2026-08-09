package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import kotlin.math.ceil

internal class WaterRenderer(
    imgParams: ImgParameters,
) : PixelRenderer(imgParams) {
    override fun renderActiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        val cellSize = imgParams.cellSize
        val cornerRadius = cellSize.toDouble() * 2 / 3

        gfx.fill(
            RoundRectangle2D.Double(
                0.0,
                0.0,
                cellSize.toDouble(),
                cellSize.toDouble(),
                cornerRadius,
                cornerRadius,
            ),
        )

        if (hasLeftNeighbour(context)) {
            val path = "h 70 v 140 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (hasTopNeighbour(context)) {
            val path = "h 140 v 70 h -140 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (hasRightNeighbour(context)) {
            val path = "m 70,0 h 70 v 140 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (hasBottomNeighbour(context)) {
            val path = "m 0,70 h 140 v 70 h -140 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }
    }

    override fun renderInactiveShape(
        imgParams: ImgParameters,
        context: PixelContext,
        gfx: Graphics2D,
    ) {
        val cellSize = imgParams.cellSize
        val cornerRadius = ceil(cellSize.toDouble() * 2 / 3)

        val hasLeft: Boolean = hasLeftNeighbour(context)
        val hasTop: Boolean = hasTopNeighbour(context)
        val hasRight: Boolean = hasRightNeighbour(context)
        val hasBottom: Boolean = hasBottomNeighbour(context)
        val globalClip = gfx.getClip()

        val corners = Area(Rectangle(0, 0, cellSize, cellSize))
        corners.subtract(
            Area(
                RoundRectangle2D.Double(
                    0.0,
                    0.0,
                    cellSize.toDouble(),
                    cellSize.toDouble(),
                    cornerRadius,
                    cornerRadius,
                ),
            ),
        )
        gfx.clip(corners)

        if (hasLeft && hasTop && context.isNeighbourSet(PixelContext.Direction.NW)) {
            val path = "h 70 v 70 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (hasTop && hasRight && context.isNeighbourSet(PixelContext.Direction.NE)) {
            val path = "m 70,0 h 70 v 70 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (hasRight && hasBottom && context.isNeighbourSet(PixelContext.Direction.SE)) {
            val path = "m 70,70 h 70 v 70 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        if (hasBottom && hasLeft && context.isNeighbourSet(PixelContext.Direction.SW)) {
            val path = "m 0,70 h 70 v 70 h -70 z"
            renderPixelFromSvgPath(imgParams, gfx, path)
        }

        gfx.setClip(globalClip)
    }

    companion object {
        private fun hasLeftNeighbour(context: PixelContext): Boolean = context.isNeighbourSet(PixelContext.Direction.W)

        private fun hasTopNeighbour(context: PixelContext): Boolean = context.isNeighbourSet(PixelContext.Direction.N)

        private fun hasRightNeighbour(context: PixelContext): Boolean = context.isNeighbourSet(PixelContext.Direction.E)

        private fun hasBottomNeighbour(context: PixelContext): Boolean = context.isNeighbourSet(PixelContext.Direction.S)
    }
}
