/*
 * Copyright (C) 2010 ZXing authors
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
package com.yepngo.qrgen.internal.zxing.common

import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

/**
 * Character-set constants used by QR encoding.
 *
 * @author Sean Owen
 */
internal object StringUtils {
    val SHIFT_JIS_CHARSET: Charset?

    init {
        var shiftJisCharset: Charset?
        try {
            shiftJisCharset = Charset.forName("SJIS")
        } catch (exception: UnsupportedCharsetException) {
            shiftJisCharset = null
        }
        SHIFT_JIS_CHARSET = shiftJisCharset
    }
}
