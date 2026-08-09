package com.yepngo.qrgen.renderers.common

import java.awt.Shape
import java.awt.geom.Path2D
import java.util.Scanner
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

internal class SvgPath2D
    @JvmOverloads
    constructor(
        xPos: Double = 0.0,
        yPos: Double = 0.0,
    ) {
        val path: Path2D.Double
        private var xPos = 0.0
        private var yPos = 0.0
        private var xStart = 0.0
        private var yStart = 0.0

        init {
            this.path = Path2D.Double()
            M(xPos, yPos)
        }

        fun M(
            x: Double,
            y: Double,
        ) {
            xPos = x
            yPos = y
            moveTo()
        }

        fun m(
            dx: Double,
            dy: Double,
        ) {
            xPos += dx
            yPos += dy
            moveTo()
        }

        fun C(vararg coords: Double?) {
            require(coords.size % 6 == 0) { "Wrong number of coordinates given to curve command" }

            path.curveTo(
                coords[0]!!,
                coords[1]!!,
                coords[2]!!,
                coords[3]!!,
                coords[4]!!,
                coords[5]!!,
            )
            xPos = coords[4]!!
            yPos = coords[5]!!

            if (coords.size > 6) {
                C(*coords.copyOfRange(6, coords.size))
            }
        }

        fun c(vararg coords: Double?) {
            require(coords.size % 6 == 0) { "Wrong number of coordinates given to curve command" }

            path.curveTo(
                coords[0]!! + xPos,
                coords[1]!! + yPos,
                coords[2]!! + xPos,
                coords[3]!! + yPos,
                coords[4]!! + xPos,
                coords[5]!! + yPos,
            )
            xPos += coords[4]!!
            yPos += coords[5]!!

            if (coords.size > 6) {
                c(*coords.copyOfRange(6, coords.size))
            }
        }

        fun l(
            dx: Double,
            dy: Double,
        ) {
            xPos += dx
            yPos += dy
            lineTo()
        }

        fun L(
            x: Double,
            y: Double,
        ) {
            xPos = x
            yPos = y
            lineTo()
        }

        fun H(x: Double) {
            xPos = x
            lineTo()
        }

        fun h(dx: Double) {
            xPos += dx
            lineTo()
        }

        private fun V(y: Double) {
            yPos = y
            lineTo()
        }

        private fun v(dy: Double) {
            yPos += dy
            lineTo()
        }

        private fun lineTo() {
            path.lineTo(xPos, yPos)
        }

        private fun moveTo() {
            path.moveTo(xPos, yPos)
            xStart = xPos
            yStart = yPos
        }

        fun closePath() {
            z()
        }

        fun z() {
            path.closePath()
            xPos = xStart
            yPos = yStart
        }

        fun d(path: String) {
            var path = path
            while (!path.isEmpty()) {
                path = extractAndProcessCommand(path)!!
            }
        }

        private fun extractAndProcessCommand(path: String): String? {
            var path = path
            path = svgTrimFront(path)
            if (path.isEmpty()) return ""

            val cmd = path.get(0)
            val tail = svgTrimFront(path.substring(1))

            when (cmd) {
                'm' -> {
                    return commandWithTwoParams(tail, BiConsumer { dx: Double?, dy: Double? -> this.m(dx!!, dy!!) })
                }

                'M' -> {
                    return commandWithTwoParams(tail, BiConsumer { x: Double?, y: Double? -> this.M(x!!, y!!) })
                }

                'l' -> {
                    return commandWithTwoParams(tail, BiConsumer { dx: Double?, dy: Double? -> this.l(dx!!, dy!!) })
                }

                'L' -> {
                    return commandWithTwoParams(tail, BiConsumer { x: Double?, y: Double? -> this.L(x!!, y!!) })
                }

                'c' -> {
                    val params = extractModuloSixDoubles(tail)
                    c(*params.p.toTypedArray<Double?>())
                    return params.tail
                }

                'C' -> {
                    val params = extractModuloSixDoubles(tail)
                    C(*params.p.toTypedArray<Double?>())
                    return params.tail
                }

                'z', 'Z' -> {
                    z()
                    return tail
                }

                'V' -> {
                    return commandWithOneParam(tail, Consumer { y: Double? -> this.V(y!!) })
                }

                'v' -> {
                    return commandWithOneParam(tail, Consumer { dy: Double? -> this.v(dy!!) })
                }

                'H' -> {
                    return commandWithOneParam(tail, Consumer { x: Double? -> this.H(x!!) })
                }

                'h' -> {
                    return commandWithOneParam(tail, Consumer { dx: Double? -> this.h(dx!!) })
                }

                else -> {
                    throw IllegalArgumentException("Unknown svg command '" + cmd + "' found")
                }
            }
        }

        private fun commandWithOneParam(
            tail: String,
            fct: Consumer<Double?>,
        ): String {
            val params = extractDoubles(tail, 1)
            fct.accept(params.p.get(0))
            return params.tail
        }

        private fun commandWithTwoParams(
            tail: String,
            fct: BiConsumer<Double?, Double?>,
        ): String {
            val params = extractDoubles(tail, 2)
            fct.accept(params.p.get(0), params.p.get(1))
            return params.tail
        }

        private fun svgTrimFront(string: String): String {
            for (idx in 0..<string.length) {
                when (string.get(idx)) {
                    ' ', ',' -> {}

                    else -> {
                        return string.substring(idx)
                    }
                }
            }
            return ""
        }

        private fun extractDoubles(
            string: String,
            numToExtract: Int,
        ): Parameters {
            when (numToExtract) {
                0 -> {
                    return Parameters(ArrayList<Double?>(), string)
                }

                1 -> {
                    return extractNextDouble(string)
                }

                else -> {
                    val firstParam = extractNextDouble(string)
                    val others = extractDoubles(firstParam.tail, numToExtract - 1)
                    others.p.add(0, firstParam.p.get(0))
                    return others
                }
            }
        }

        private fun extractNextDouble(string: String): Parameters {
            var string = string
            string = svgTrimFront(string)
            if (string.isEmpty()) {
                return Parameters(ArrayList<Double?>(), "")
            }

            val scanner = Scanner(string)
            scanner.useDelimiter("[, ]+")
            val token = scanner.next("^[+\\-]?[0-9]{1,13}(\\.[0-9]*)?(e[+\\\\-]?[0-9]{1,3})?")
            require(!(token == null || token.isEmpty())) { "Didn't find expected double value" }

            val p: MutableList<Double?> = ArrayList<Double?>(1)
            p.add(token.toDouble())
            return Parameters(p, string.substring(token.length))
        }

        private fun extractModuloSixDoubles(string: String): Parameters {
            val params = extractDoubles(string, 6)
            val tail = svgTrimFront(params.tail)
            if (tail.isEmpty() || tail.matches("^[^0-9+\\-].*".toRegex())) {
                return params
            }
            val others = extractModuloSixDoubles(tail)
            return Parameters(
                Stream
                    .concat<Double?>(params.p.stream(), others.p.stream())
                    .collect(Collectors.toList()),
                others.tail,
            )
        }

        private class Parameters(
            val p: MutableList<Double?>,
            val tail: String,
        )

        companion object {
            fun drawSvgCommand(svgCommand: String): Shape {
                val svg = SvgPath2D()
                svg.d(svgCommand)
                svg.closePath()
                return svg.path
            }
        }
    }
