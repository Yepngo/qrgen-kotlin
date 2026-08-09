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
package com.yepngo.qrgen.internal.zxing.qrcode.encoder

import com.yepngo.qrgen.internal.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.Mode
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.Version

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
internal class QRCode {
    /**
     * @return the mode. Not relevant if [com.yepngo.qrgen.internal.zxing.EncodeHintType.QR_COMPACT] is selected.
     */
    var mode: Mode? = null
    var eCLevel: ErrorCorrectionLevel? = null
    var version: Version? = null
    var maskPattern: Int
    var matrix: ByteMatrix? = null

    init {
        maskPattern = -1
    }

    override fun toString(): String {
        val result = StringBuilder(200)
        result.append("<<\n")
        result.append(" mode: ")
        result.append(mode)
        result.append("\n ecLevel: ")
        result.append(this.eCLevel)
        result.append("\n version: ")
        result.append(version)
        result.append("\n maskPattern: ")
        result.append(maskPattern)
        if (matrix == null) {
            result.append("\n matrix: null\n")
        } else {
            result.append("\n matrix:\n")
            result.append(matrix)
        }
        result.append(">>\n")
        return result.toString()
    }

    companion object {
        const val NUM_MASK_PATTERNS: Int = 8

        // Check if "mask_pattern" is valid.
        fun isValidMaskPattern(maskPattern: Int): Boolean = maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS
    }
}
