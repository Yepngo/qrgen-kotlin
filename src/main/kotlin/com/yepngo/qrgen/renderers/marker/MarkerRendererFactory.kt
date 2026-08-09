package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.config.MarkerStyle

internal object MarkerRendererFactory {
    fun create(markerStyle: MarkerStyle): MarkerRenderer {
        when (markerStyle) {
            MarkerStyle.RECTANGLES -> return RectangleRenderer()
            MarkerStyle.ROUND_CORNERS -> return RoundCornersRenderer()
            MarkerStyle.CIRCLES -> return CirclesRenderer()
            MarkerStyle.DROP_IN -> return DropInRenderer()
            MarkerStyle.DROP_OUT -> return DropOutRenderer()
            MarkerStyle.ROUND_IN -> return RoundInRenderer()
            MarkerStyle.ROUND_OUT -> return RoundOutRenderer()
            MarkerStyle.EDGE_IN -> return EdgeInRenderer()
            MarkerStyle.EDGE_OUT -> return EdgeOutRenderer()
        }
        throw RuntimeException("case not handled")
    }
}
