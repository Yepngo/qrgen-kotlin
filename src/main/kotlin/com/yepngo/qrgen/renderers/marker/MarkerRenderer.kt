package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import java.awt.Graphics2D

internal abstract class MarkerRenderer {
    fun render(
        gfx: Graphics2D,
        imgParams: ImgParameters,
    ) {
        val markerOffset: Int =
            (imgParams.matrixWidthInCells - SIZE_OF_POSITION_MARKER) * imgParams.cellSize

        val cellSize = imgParams.cellSize

        renderTopLeftMarker(gfx, cellSize, imgParams)
        gfx.translate(markerOffset, 0)
        renderTopRightMarker(gfx, cellSize, imgParams)
        gfx.translate(-markerOffset, markerOffset)
        renderBottomLeftMarker(gfx, cellSize, imgParams)
    }

    protected abstract fun renderTopLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    )

    protected open fun renderTopRightMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        renderTopLeftMarker(gfx, cellSize, imgParams)
    }

    protected open fun renderBottomLeftMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        renderTopLeftMarker(gfx, cellSize, imgParams)
    }

    protected fun renderRotated(
        angle: Double,
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
        renderer: SingleMarkerRenderer,
    ) {
        val transform = gfx.getTransform()
        val halfMarkerSize: Int = (cellSize * SIZE_OF_POSITION_MARKER) / 2
        gfx.translate(halfMarkerSize, halfMarkerSize)
        gfx.rotate(angle)
        gfx.translate(-halfMarkerSize, -halfMarkerSize)
        renderer.renderMarker(gfx, cellSize, imgParams)
        gfx.setTransform(transform)
    }

    protected fun interface SingleMarkerRenderer {
        fun renderMarker(
            gfx: Graphics2D,
            cellSize: Int,
            imgParams: ImgParameters,
        )
    }

    companion object {
        // regardless of the size of the payload or the error correction level
        // the position markers will always be seven pixels high and wide
        // (tested with ZXing 3.5.0)
        const val SIZE_OF_POSITION_MARKER: Int = 7
    }
}
