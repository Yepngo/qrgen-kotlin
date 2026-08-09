package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.config.PixelStyle
import com.yepngo.qrgen.renderers.common.ImgParameters

internal object PixelRendererFactory {
    fun generate(
        pixelStyle: PixelStyle,
        imgParams: ImgParameters,
    ): PixelRenderer {
        when (pixelStyle) {
            PixelStyle.RECTANGLES -> return DefaultRenderer(imgParams)
            PixelStyle.SMALL_RECTANGLES -> return SmallRectanglesRenderer(imgParams)
            PixelStyle.DOTS -> return CirclesRenderer(imgParams)
            PixelStyle.ROUND_CORNERS -> return RoundCornersRenderer(imgParams)
            PixelStyle.ROWS -> return RowsRenderer(imgParams)
            PixelStyle.COLUMNS -> return ColumnsRenderer(imgParams)
            PixelStyle.SNAKES -> return SnakesRenderer(imgParams)
            PixelStyle.WATER -> return WaterRenderer(imgParams)
        }
        throw RuntimeException("case not handled")
    }
}
