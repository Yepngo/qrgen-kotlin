package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal class EdgeOutRenderer : MarkerRenderer() {
    private val edgeRenderer = SingleEdgeRenderer()

    protected override fun renderTopLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        edgeRenderer.renderSingleMarker(gfx, cellSize, imgParams)
    }

    protected override fun renderTopRightMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        renderRotated(
            Math.PI / 2.0,
            gfx,
            cellSize,
            imgParams,
            { gfx: Graphics2D, cellSize: Int, imgParams: ImgParameters ->
                edgeRenderer.renderSingleMarker(
                    gfx,
                    cellSize,
                    imgParams,
                )
            },
        )
    }

    protected override fun renderBottomLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        renderRotated(
            Math.PI / -2.0,
            gfx,
            cellSize,
            imgParams,
            { gfx: Graphics2D, cellSize: Int, imgParams: ImgParameters ->
                edgeRenderer.renderSingleMarker(
                    gfx,
                    cellSize,
                    imgParams,
                )
            },
        )
    }
}
