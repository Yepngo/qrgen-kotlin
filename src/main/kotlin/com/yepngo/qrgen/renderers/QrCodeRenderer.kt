package com.yepngo.qrgen.renderers

import com.yepngo.qrgen.config.MarkerStyle
import com.yepngo.qrgen.config.PixelStyle
import com.yepngo.qrgen.internal.zxing.EncodeHintType
import com.yepngo.qrgen.internal.zxing.WriterException
import com.yepngo.qrgen.internal.zxing.common.BitArray
import com.yepngo.qrgen.internal.zxing.common.BitMatrix
import com.yepngo.qrgen.internal.zxing.qrcode.QRCodeWriter
import com.yepngo.qrgen.renderers.common.ImgParameters
import com.yepngo.qrgen.renderers.marker.MarkerRenderer
import com.yepngo.qrgen.renderers.marker.MarkerRendererFactory
import com.yepngo.qrgen.renderers.pixel.PixelContext
import com.yepngo.qrgen.renderers.pixel.PixelRenderer
import com.yepngo.qrgen.renderers.pixel.PixelRendererFactory
import com.yepngo.qrgen.utils.ColorConfig
import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.floor
import kotlin.math.min

internal class QrCodeRenderer(
    pixelStyle: PixelStyle,
    markerStyle: MarkerStyle,
) {
    private val writer: QRCodeWriter
    private var markerRenderer: MarkerRenderer

    private var pixelStyle: PixelStyle

    init {
        this.writer = QRCodeWriter()
        this.markerRenderer = MarkerRendererFactory.create(markerStyle)
        this.pixelStyle = pixelStyle
    }

    fun setMarkerStyle(markerStyle: MarkerStyle) {
        this.markerRenderer = MarkerRendererFactory.create(markerStyle)
    }

    fun setPixelStyle(pixelStyle: PixelStyle) {
        this.pixelStyle = pixelStyle
    }

    @Throws(WriterException::class)
    fun encodeAndRender(
        payload: String,
        colorConfig: ColorConfig,
        width: Int,
        height: Int,
        encodingHints: MutableMap<EncodeHintType?, *>,
    ): BufferedImage {
        val margin = getMargin(encodingHints)
        val hintsCopy: MutableMap<EncodeHintType?, Any?> = HashMap<EncodeHintType?, Any?>(encodingHints)
        // ZXing would only compute the QRCode without any border (margin);
        // the margin is then afterward applied by this method
        val zero = 0
        hintsCopy.put(EncodeHintType.MARGIN, zero)

        val matrix = writer.encode(payload, 1, 1, hintsCopy)
        val imgParams = computeImageParameters(width, height, matrix, margin, colorConfig)
        val img = drawCanvas(width, height, colorConfig)

        val gfx = img.createGraphics()
        gfx.setComposite(AlphaComposite.Src)
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        gfx.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        gfx.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        gfx.translate(imgParams.firstCellX, imgParams.firstCellY)

        val transform = gfx.getTransform()
        renderMatrix(matrix, gfx, imgParams)
        gfx.setTransform(transform)
        markerRenderer.render(gfx, imgParams)

        gfx.dispose()
        return img
    }

    private fun renderMatrix(
        matrix: BitMatrix,
        gfx: Graphics2D,
        imgParams: ImgParameters,
    ) {
        val renderer = PixelRendererFactory.generate(pixelStyle, imgParams)
        gfx.setColor(imgParams.onColorForAwt)
        applyQrCodePixels(gfx, matrix, renderer, imgParams)
    }

    private fun applyQrCodePixels(
        gfx: Graphics2D,
        matrix: BitMatrix,
        renderer: PixelRenderer,
        imgParams: ImgParameters,
    ) {
        var top: BitArray?
        var mid: BitArray? = null
        var bottom: BitArray? = matrix.getRow(0, null)

        val detector = PositionMarkerDetector(matrix.width)

        for (yCoord in 0..<matrix.height) {
            top = mid
            mid = bottom
            bottom = if (yCoord >= (matrix.height - 1)) null else matrix.getRow(yCoord + 1, null)
            val context = PixelContext(matrix.width, top, checkNotNull(mid), bottom)

            for (xCoord in 0..<matrix.width) {
                if (!detector.detected(xCoord, yCoord)) {
                    renderer.renderPixel(context, gfx)
                }
                gfx.translate(imgParams.cellSize, 0)
                context.shiftRight()
            }

            gfx.translate(-1 * imgParams.matrixWidthInCells * imgParams.cellSize, imgParams.cellSize)
        }
    }

    private fun drawCanvas(
        width: Int,
        height: Int,
        colorConfig: ColorConfig,
    ): BufferedImage {
        val canvas = BufferedImage(width, height, colorConfig.determineImageType())

        val gfx = canvas.createGraphics()
        gfx.setColor(colorConfig.offColorForAwt)
        gfx.fillRect(0, 0, width, height)
        gfx.dispose()

        return canvas
    }

    private fun getMargin(encodingHints: MutableMap<EncodeHintType?, *>): Int {
        val margin = encodingHints.get(EncodeHintType.MARGIN) as Int?
        return if (margin == null) DEFAULT_MARGIN_FROM_ZXING else margin
    }

    @Throws(WriterException::class)
    private fun computeImageParameters(
        width: Int,
        height: Int,
        matrix: BitMatrix,
        margin: Int,
        colorConfig: ColorConfig,
    ): ImgParameters {
        val targetSize = min(width, height).toDouble()
        val numCellsOfCode = matrix.width.toDouble()
        val numCellsOfCodeAndMargin = numCellsOfCode + (2 * margin)

        if (targetSize < numCellsOfCodeAndMargin) {
            // A rendered QR code will not fit into the requested boundaries
            throw WriterException("Requested width/height is too small for generated QR Code")
        }

        val pixelPerCell = floor(targetSize / numCellsOfCodeAndMargin).toInt()
        val sizeOfCodeInPixels = (pixelPerCell * numCellsOfCode).toInt()
        return ImgParameters(
            pixelPerCell,
            matrix.width,
            (width - sizeOfCodeInPixels) / 2,
            (height - sizeOfCodeInPixels) / 2,
            colorConfig,
        )
    }

    private class PositionMarkerDetector(
        matrixSize: Int,
    ) {
        private val xStartOfLeftMarker: Int
        private val yStartOfLowerMarker: Int

        init {
            xStartOfLeftMarker = matrixSize - SIZE_OF_POSITION_MARKER
            yStartOfLowerMarker = matrixSize - SIZE_OF_POSITION_MARKER
        }

        fun detected(
            xCoord: Int,
            yCoord: Int,
        ): Boolean {
            if (xCoord < SIZE_OF_POSITION_MARKER) {
                return (yCoord < SIZE_OF_POSITION_MARKER) ||
                    (yCoord >= yStartOfLowerMarker)
            }

            return (xCoord >= xStartOfLeftMarker) &&
                (yCoord < SIZE_OF_POSITION_MARKER)
        }
    }

    companion object {
        // as stated by the code of ZXing 3.5.0
        private const val DEFAULT_MARGIN_FROM_ZXING = 4

        // regardless of the size of the payload or the error correction level
        // the position markers will always be seven pixels high and wide
        // (tested with ZXing 3.5.0)
        private const val SIZE_OF_POSITION_MARKER = 7
    }
}
