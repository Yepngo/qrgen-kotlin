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

import java.util.Arrays

/**
 * JAVAPORT: The original code was a 2D array of ints, but since it only ever gets assigned
 * -1, 0, and 1, I'm going to use less memory and go with bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
internal class ByteMatrix(
    val width: Int,
    val height: Int,
) {
    /**
     * @return an internal representation as bytes, in row-major order. array[y][x] represents point (x,y)
     */
    val array: Array<ByteArray>

    init {
        this.array = Array(height) { ByteArray(width) }
    }

    fun get(
        x: Int,
        y: Int,
    ): Byte = this.array[y][x]

    fun set(
        x: Int,
        y: Int,
        value: Byte,
    ) {
        this.array[y][x] = value
    }

    fun set(
        x: Int,
        y: Int,
        value: Int,
    ) {
        this.array[y][x] = value.toByte()
    }

    fun set(
        x: Int,
        y: Int,
        value: Boolean,
    ) {
        this.array[y][x] = (if (value) 1 else 0).toByte()
    }

    fun clear(value: Byte) {
        for (aByte in this.array) {
            Arrays.fill(aByte, value)
        }
    }

    override fun toString(): String {
        val result = StringBuilder(2 * width * height + 2)
        for (y in 0..<height) {
            val bytesY = this.array[y]
            for (x in 0..<width) {
                when (bytesY[x]) {
                    0.toByte() -> result.append(" 0")
                    1.toByte() -> result.append(" 1")
                    else -> result.append("  ")
                }
            }
            result.append('\n')
        }
        return result.toString()
    }
}
