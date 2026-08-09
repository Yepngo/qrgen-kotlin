package com.yepngo.qrgen.renderers.pixel;

import com.yepngo.qrgen.renderers.common.ImgParameters;

import java.awt.*;

public class CirclesRenderer extends IndependentPixelRenderer {
    public CirclesRenderer(ImgParameters imgParams) {
        super(imgParams);
    }

    @Override
    protected void drawActualShape(ImgParameters imgParams, Graphics2D gfx) {
        gfx.fillOval(0, 0, imgParams.getCellSize(), imgParams.getCellSize());
    }
}
