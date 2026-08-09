package com.yepngo.qrgen.renderers.marker

import com.yepngo.qrgen.renderers.common.ImgParameters
import com.yepngo.qrgen.renderers.common.SvgPath2D.Companion.drawSvgCommand
import java.awt.Graphics2D
import java.awt.geom.AffineTransform

internal class SingleRoundRenderer {
    fun renderSingleMarker(
        gfx: Graphics2D,
        cellSize: Int,
        imgParams: ImgParameters,
    ) {
        val markerSize = cellSize * MarkerRenderer.SIZE_OF_POSITION_MARKER
        val innerMarkerSize = cellSize * (MarkerRenderer.SIZE_OF_POSITION_MARKER - 4)

        val transform = gfx.getTransform()

        val factor = markerSize / 82.0
        gfx.scale(factor, factor)
        gfx.transform(AffineTransform(1.3333333, 0.0, 0.0, -1.3333333, 0.0, 81.866667))

        gfx.scale(0.1, 0.1)
        gfx.setColor(imgParams.outerMarkerColorForAwt)
        gfx.fill(drawSvgCommand(OUTER_PATH))
        gfx.setTransform(transform)

        gfx.setColor(imgParams.innerMarkerColorForAwt)
        gfx.fillOval(cellSize * 2, cellSize * 2, innerMarkerSize, innerMarkerSize)
    }

    companion object {
        private val OUTER_PATH =
            "m 110.496,502.5 h 0.102 z M 89.2161,525.30762 C 87.137691,379.71098 " +
                "87.461237,234.1132 87.669783,88.505788 c 0,0 158.316757,-1.091748 218.904757,-1.091748 62.03033,0 " +
                "112.98164,21.18772 155.77484,64.77704 42.62765,43.43049 64.24384,95.36184 64.24384,154.35969 " +
                "0,60.39149 -0.21908,218.9304 -0.21908,218.9304 -151.98713,1.69764 -282.40756,0.37813 " +
                "-437.15804,-0.17355 z M 306.504,0 C 221.648,0 0.59596959,-0.64455722 0.59596959,-0.64455722 " +
                "-0.08241598,203.68048 5.032627e-4,409.74154 0.00589224,614.06743 204.41818,616.35877 " +
                "410.56503,615.56192 614.97828,613.95532 c 0,0 -0.97428,-223.03332 -0.97428,-307.45532 0,-82.633 " +
                "-30.098,-155.191 -89.453,-215.6641 C 465.383,30.5625 392.02,0 306.504,0"
    }
}
