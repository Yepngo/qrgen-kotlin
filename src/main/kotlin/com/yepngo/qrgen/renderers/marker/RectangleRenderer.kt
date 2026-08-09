package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class RectangleRenderer : MarkerRenderer() {
    override fun renderTopLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        val markerSize = cellSize * SIZE_OF_POSITION_MARKER
        val whiteSize = cellSize * (SIZE_OF_POSITION_MARKER - 2)
        val innerSize = cellSize * (SIZE_OF_POSITION_MARKER - 4)

        gfx.setColor(imgParams.outerMarkerColorForAwt)
        gfx.fillRect(0, 0, markerSize, markerSize)

        gfx.setColor(imgParams.offColorForAwt)
        gfx.fillRect(cellSize, cellSize, whiteSize, whiteSize)

        gfx.setColor(imgParams.innerMarkerColorForAwt)
        gfx.fillRect(2 * cellSize, 2 * cellSize, innerSize, innerSize)
    }
}
