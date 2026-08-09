package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D
import java.awt.geom.RoundRectangle2D

internal class RoundCornersRenderer : MarkerRenderer() {
    override fun renderTopLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        val markerSize = cellSize * SIZE_OF_POSITION_MARKER
        val whiteSize = cellSize * (SIZE_OF_POSITION_MARKER - 2)
        val innerSize = cellSize * (SIZE_OF_POSITION_MARKER - 4)
        val arc = (2 * cellSize).toDouble()

        gfx.setColor(imgParams.outerMarkerColorForAwt)
        gfx.fill(RoundRectangle2D.Double(0.0, 0.0, markerSize.toDouble(), markerSize.toDouble(), 2 * arc, 2 * arc))

        gfx.setColor(imgParams.offColorForAwt)
        gfx.fill(
            RoundRectangle2D.Double(
                cellSize.toDouble(),
                cellSize.toDouble(),
                whiteSize.toDouble(),
                whiteSize.toDouble(),
                arc,
                arc,
            ),
        )

        gfx.setColor(imgParams.innerMarkerColorForAwt)
        gfx.fill(
            RoundRectangle2D.Double(
                (2 * cellSize).toDouble(),
                (2 * cellSize).toDouble(),
                innerSize.toDouble(),
                innerSize.toDouble(),
                arc,
                arc,
            ),
        )
    }
}
