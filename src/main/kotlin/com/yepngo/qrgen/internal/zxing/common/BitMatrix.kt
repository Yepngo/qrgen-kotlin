/*
 * Copyright 2007 ZXing authors
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

/**
 * Compact two-dimensional bit matrix used by QR encoding and rendering.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
internal class BitMatrix(
    width: Int,
    height: Int,
) {
    val width: Int
    val height: Int
    private val rowSize: Int
    private val bits: IntArray

    constructor(dimension: Int) : this(dimension, dimension)

    init {
        require(!(width < 1 || height < 1)) { "Both dimensions must be greater than 0" }
        this.width = width
        this.height = height
        this.rowSize = (width + 31) / 32
        this.bits = IntArray(rowSize * height)
    }

    fun get(
        x: Int,
        y: Int,
    ): Boolean {
        val offset = y * rowSize + (x / 32)
        return ((bits[offset] ushr (x and 0x1f)) and 1) != 0
    }

    fun set(
        x: Int,
        y: Int,
    ) {
        val offset = y * rowSize + (x / 32)
        bits[offset] = bits[offset] or (1 shl (x and 0x1f))
    }

    fun clear() {
        for (i in bits.indices) {
            bits[i] = 0
        }
    }

    fun setRegion(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ) {
        require(!(top < 0 || left < 0)) { "Left and top must be nonnegative" }
        require(!(height < 1 || width < 1)) { "Height and width must be at least 1" }
        val right = left + width
        val bottom = top + height
        require(!(bottom > this.height || right > this.width)) { "The region must fit inside the matrix" }
        for (y in top..<bottom) {
            val offset = y * rowSize
            for (x in left..<right) {
                bits[offset + (x / 32)] = bits[offset + (x / 32)] or (1 shl (x and 0x1f))
            }
        }
    }

    fun getRow(
        y: Int,
        row: BitArray?,
    ): BitArray {
        var row = row
        if (row == null || row.size < width) {
            row = BitArray(width)
        } else {
            row.clear()
        }
        val offset = y * rowSize
        for (x in 0..<rowSize) {
            row.setBulk(x * 32, bits[offset + x])
        }
        return row
    }
}
