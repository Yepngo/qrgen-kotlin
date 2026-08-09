/*
 * Copyright 2008 ZXing authors
 * Modified for qrgen: relocated and reduced to QR-generation functionality.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yepngo.qrgen.internal.zxing.qrcode

import com.yepngo.qrgen.internal.zxing.EncodeHintType
import com.yepngo.qrgen.internal.zxing.WriterException
import com.yepngo.qrgen.internal.zxing.common.BitMatrix
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.yepngo.qrgen.internal.zxing.qrcode.encoder.Encoder
import com.yepngo.qrgen.internal.zxing.qrcode.encoder.QRCode
import kotlin.math.max
import kotlin.math.min

/**
 * This object renders a QR Code as a BitMatrix 2D array of greyscale values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
internal class QRCodeWriter {
    @JvmOverloads
    @Throws(WriterException::class)
    fun encode(
        contents: String,
        width: Int,
        height: Int,
        hints: MutableMap<EncodeHintType?, *>? = null,
    ): BitMatrix {
        require(!contents.isEmpty()) { "Found empty contents" }

        require(!(width < 0 || height < 0)) {
            "Requested dimensions are too small: " + width + 'x' +
                height
        }

        var errorCorrectionLevel = ErrorCorrectionLevel.L
        var quietZone: Int = QUIET_ZONE_SIZE
        if (hints != null) {
            if (hints.containsKey(EncodeHintType.ERROR_CORRECTION)) {
                errorCorrectionLevel =
                    ErrorCorrectionLevel.valueOf(hints.get(EncodeHintType.ERROR_CORRECTION).toString())
            }
            if (hints.containsKey(EncodeHintType.MARGIN)) {
                quietZone = hints.get(EncodeHintType.MARGIN).toString().toInt()
            }
        }

        val code = Encoder.encode(contents, errorCorrectionLevel, hints)
        return renderResult(code, width, height, quietZone)
    }

    companion object {
        private const val QUIET_ZONE_SIZE = 4

        // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
        // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
        private fun renderResult(
            code: QRCode,
            width: Int,
            height: Int,
            quietZone: Int,
        ): BitMatrix {
            val input = code.matrix
            checkNotNull(input)
            val inputWidth = input.width
            val inputHeight = input.height
            val qrWidth = inputWidth + (quietZone * 2)
            val qrHeight = inputHeight + (quietZone * 2)
            val outputWidth = max(width, qrWidth)
            val outputHeight = max(height, qrHeight)

            val multiple = min(outputWidth / qrWidth, outputHeight / qrHeight)
            // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
            // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
            // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
            // handle all the padding from 100x100 (the actual QR) up to 200x160.
            val leftPadding = (outputWidth - (inputWidth * multiple)) / 2
            val topPadding = (outputHeight - (inputHeight * multiple)) / 2

            val output = BitMatrix(outputWidth, outputHeight)

            var inputY = 0
            var outputY = topPadding
            while (inputY < inputHeight) {
                // Write the contents of this row of the QR code
                var inputX = 0
                var outputX = leftPadding
                while (inputX < inputWidth) {
                    if (input.get(inputX, inputY).toInt() == 1) {
                        output.setRegion(outputX, outputY, multiple, multiple)
                    }
                    inputX++
                    outputX += multiple
                }
                inputY++
                outputY += multiple
            }

            return output
        }
    }
}
