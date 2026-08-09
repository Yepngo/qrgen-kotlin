package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.renderers.common.ImgParameters

internal class DefaultRenderer(
    imgParams: ImgParameters,
) : // the base class has a default implementation which already renders a filled cell
// so nothing to be done here
    IndependentPixelRenderer(imgParams)
