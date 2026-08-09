package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class DropOutRenderer : MarkerRenderer() {
    private val dropRenderer = SingleDropRenderer()

    override fun renderTopLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        renderRotated(
            Math.PI / -2.0,
            gfx,
            cellSize,
            imgParams,
            SingleMarkerRenderer { gfx: Graphics2D, cellSize: Int, imgParams: ImgParameters ->
                dropRenderer.renderSingleMarker(
                    gfx!!,
                    cellSize,
                    imgParams!!,
                )
            },
        )
    }

    override fun renderTopRightMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        dropRenderer.renderSingleMarker(gfx, cellSize, imgParams)
    }

    override fun renderBottomLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        renderRotated(
            Math.PI,
            gfx,
            cellSize,
            imgParams,
            SingleMarkerRenderer { gfx: Graphics2D, cellSize: Int, imgParams: ImgParameters ->
                dropRenderer.renderSingleMarker(
                    gfx!!,
                    cellSize,
                    imgParams!!,
                )
            },
        )
    }
}
