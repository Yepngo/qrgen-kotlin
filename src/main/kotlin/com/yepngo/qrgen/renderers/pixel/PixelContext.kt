package com.yepngo.qrgen.renderers.pixel

import com.yepngo.qrgen.internal.zxing.common.BitArray

internal class PixelContext(
    private val rowWidth: Int,
    top: BitArray?,
    mid: BitArray,
    bottom: BitArray?,
) {
    private val top: BitArray?
    private val mid: BitArray
    private val bottom: BitArray?

    private var column = 0

    init {
        this.top = top
        this.mid = mid
        this.bottom = bottom
    }

    fun shiftRight() {
        ++column
    }

    val isSet: Boolean
        get() = mid.get(column)

    fun isNeighbourSet(direction: Direction): Boolean {
        when (direction) {
            Direction.NW -> return top != null && (column != 0 && top.get(column - 1))
            Direction.N -> return top != null && top.get(column)
            Direction.NE -> return top != null && (column < (rowWidth - 1) && top.get(column + 1))
            Direction.W -> return column != 0 && mid.get(column - 1)
            Direction.E -> return column < (rowWidth - 1) && mid.get(column + 1)
            Direction.SW -> return bottom != null && (column != 0 && bottom.get(column - 1))
            Direction.S -> return bottom != null && bottom.get(column)
            Direction.SE -> return bottom != null && (column < (rowWidth - 1) && bottom.get(column + 1))
        }
        throw RuntimeException("case not handled")
    }

    enum class Direction {
        NW,
        N,
        NE,
        W,
        E,
        SW,
        S,
        SE,
    }
}
