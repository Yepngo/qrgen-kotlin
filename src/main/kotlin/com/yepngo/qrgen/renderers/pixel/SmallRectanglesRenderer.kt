package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters

internal class SmallRectanglesRenderer(
    imgParams: ImgParameters,
) : IndependentPixelRenderer(imgParams) {
    override val svgPath: String
        get() = // a rectangle which is a bit smaller than the complete cell
            "m 10,10 h 120 v 120 h -120 z"
}
