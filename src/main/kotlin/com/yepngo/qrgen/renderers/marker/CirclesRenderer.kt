package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class CirclesRenderer : MarkerRenderer() {
    protected override fun renderTopLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        val outerBlackSize: Int = (cellSize * SIZE_OF_POSITION_MARKER)
        val whiteSize: Int = cellSize * (SIZE_OF_POSITION_MARKER - 2)
        val innerBlackSize: Int = cellSize * (SIZE_OF_POSITION_MARKER - 4)

        gfx.setColor(imgParams.outerMarkerColorForAwt)
        gfx.fillOval(0, 0, outerBlackSize, outerBlackSize)

        gfx.setColor(imgParams.offColorForAwt)
        gfx.fillOval(cellSize, cellSize, whiteSize, whiteSize)

        gfx.setColor(imgParams.innerMarkerColorForAwt)
        gfx.fillOval(cellSize * 2, cellSize * 2, innerBlackSize, innerBlackSize)
    }
}
