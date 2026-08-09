package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters

internal class RoundCornersRenderer(
    imgParams: ImgParameters,
) : IndependentPixelRenderer(imgParams) {
    override val svgPath: String =
        "m 50,0 h 40 c 27.7,0 50,22.3 50,50 v 40 c 0,27.7 -22.3,50 -50,50 H 50 " +
            "C 22.3,140 0,117.7 0,90 V 50 C 0,22.3 22.3,0 50,0 Z"
}
