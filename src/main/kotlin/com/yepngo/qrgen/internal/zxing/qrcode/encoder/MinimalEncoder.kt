/*
 * Copyright 2021 ZXing authors
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

import com.yepngo.qrgen.internal.zxing.WriterException
import com.yepngo.qrgen.internal.zxing.common.BitArray
import com.yepngo.qrgen.internal.zxing.common.ECIEncoderSet
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.Mode
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.Version
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.Version.Companion.getVersionForNumber
import java.nio.charset.Charset

/**
 * Encoder that encodes minimally
 *
 * Algorithm:
 *
 * The eleventh commandment was "Thou Shalt Compute" or "Thou Shalt Not Compute" - I forget which (Alan Perilis).
 *
 * This implementation computes. As an alternative, the QR-Code specification suggests heuristics like this one:
 *
 * If initial input data is in the exclusive subset of the Alphanumeric character set AND if there are less than
 * [6,7,8] characters followed by data from the remainder of the 8-bit byte character set, THEN select the 8-
 * bit byte mode ELSE select Alphanumeric mode;
 *
 * This is probably right for 99.99% of cases but there is at least this one counter example: The string "AAAAAAa"
 * encodes 2 bits smaller as ALPHANUMERIC(AAAAAA), BYTE(a) than by encoding it as BYTE(AAAAAAa).
 * Perhaps that is the only counter example but without having proof, it remains unclear.
 *
 * ECI switching:
 *
 * In multi language content the algorithm selects the most compact representation using ECI modes.
 * For example the most compact representation of the string "\u0150\u015C" (O-double-acute, S-circumflex) is
 * ECI(UTF-8), BYTE(\u0150\u015C) while prepending one or more times the same leading character as in
 * "\u0150\u0150\u015C", the most compact representation uses two ECIs so that the string is encoded as
 * ECI(ISO-8859-2), BYTE(\u0150\u0150), ECI(ISO-8859-3), BYTE(\u015C).
 *
 * @author Alex Geller
 */
internal class MinimalEncoder(
    private val stringToEncode: String,
    priorityCharset: Charset?,
    private val isGS1: Boolean,
    ecLevel: ErrorCorrectionLevel,
) {
    private enum class VersionSize(
        description: String,
    ) {
        SMALL("version 1-9"),
        MEDIUM("version 10-26"),
        LARGE("version 27-40"),
        ;

        private val description: String?

        init {
            this.description = description
        }

        override fun toString(): String = description!!
    }

    private val encoders: ECIEncoderSet
    private val ecLevel: ErrorCorrectionLevel

    /**
     * Creates a MinimalEncoder
     *
     * @param stringToEncode The string to encode
     * @param priorityCharset The preferred [Charset]. When the value of the argument is null, the algorithm
     * chooses charsets that leads to a minimal representation. Otherwise the algorithm will use the priority
     * charset to encode any character in the input that can be encoded by it if the charset is among the
     * supported charsets.
     * @param isGS1 `true` if a FNC1 is to be prepended; `false` otherwise
     * @param ecLevel The error correction level.
     * @see ResultList.getVersion
     */
    init {
        this.encoders = ECIEncoderSet(stringToEncode, priorityCharset, -1)
        this.ecLevel = ecLevel
    }

    @Throws(WriterException::class)
    fun encode(version: Version?): ResultList {
        if (version == null) { // compute minimal encoding trying the three version sizes.
            val versions =
                arrayOf(
                    getVersion(VersionSize.SMALL),
                    getVersion(VersionSize.MEDIUM),
                    getVersion(VersionSize.LARGE),
                )
            val results: Array<ResultList> =
                arrayOf<ResultList>(
                    encodeSpecificVersion(versions[0]),
                    encodeSpecificVersion(versions[1]),
                    encodeSpecificVersion(versions[2]),
                )
            var smallestSize = Int.MAX_VALUE
            var smallestResult = -1
            for (i in 0..2) {
                val size = results[i].size
                if (Encoder.willFit(size, versions[i], ecLevel) && size < smallestSize) {
                    smallestSize = size
                    smallestResult = i
                }
            }
            if (smallestResult < 0) {
                throw WriterException("Data too big for any version")
            }
            return results[smallestResult]
        } else { // compute minimal encoding for a given version
            val result = encodeSpecificVersion(version)
            if (!Encoder.willFit(
                    result.size,
                    getVersion(
                        Companion.getVersionSize(
                            result.version!!,
                        ),
                    ),
                    ecLevel,
                )
            ) {
                throw WriterException("Data too big for version" + version)
            }
            return result
        }
    }

    fun canEncode(
        mode: Mode,
        c: Char,
    ): Boolean {
        when (mode) {
            Mode.KANJI -> return isDoubleByteKanji(c)

            Mode.ALPHANUMERIC -> return isAlphanumeric(c)

            Mode.NUMERIC -> return isNumeric(c)

            Mode.BYTE -> return true

            // any character can be encoded as byte(s). Up to the caller to manage splitting into
            else -> return false
        }
    }

    private fun addEdge(
        edges: Array<Array<Array<Edge?>>>,
        position: Int,
        edge: Edge,
    ) {
        val vertexIndex = position + edge.characterLength
        val modeEdges: Array<Edge?> = edges[vertexIndex][edge.charsetEncoderIndex]
        val modeOrdinal: Int = getCompactedOrdinal(edge.mode)
        val existing = modeEdges[modeOrdinal]
        if (existing == null || existing.cachedTotalSize > edge.cachedTotalSize) {
            modeEdges[modeOrdinal] = edge
        }
    }

    private fun addEdges(
        version: Version,
        edges: Array<Array<Array<Edge?>>>,
        from: Int,
        previous: Edge?,
    ) {
        var start = 0
        var end = encoders.length()
        val priorityEncoderIndex = encoders.priorityEncoderIndex
        if (priorityEncoderIndex >= 0 && encoders.canEncode(stringToEncode.get(from), priorityEncoderIndex)) {
            start = priorityEncoderIndex
            end = priorityEncoderIndex + 1
        }

        for (i in start..<end) {
            if (encoders.canEncode(stringToEncode.get(from), i)) {
                addEdge(edges, from, Edge(Mode.BYTE, from, i, 1, previous, version))
            }
        }

        if (canEncode(Mode.KANJI, stringToEncode.get(from))) {
            addEdge(edges, from, Edge(Mode.KANJI, from, 0, 1, previous, version))
        }

        val inputLength = stringToEncode.length
        if (canEncode(Mode.ALPHANUMERIC, stringToEncode.get(from))) {
            addEdge(
                edges,
                from,
                Edge(
                    Mode.ALPHANUMERIC,
                    from,
                    0,
                    if (from + 1 >= inputLength ||
                        !canEncode(Mode.ALPHANUMERIC, stringToEncode.get(from + 1))
                    ) {
                        1
                    } else {
                        2
                    },
                    previous,
                    version,
                ),
            )
        }

        if (canEncode(Mode.NUMERIC, stringToEncode.get(from))) {
            addEdge(
                edges,
                from,
                Edge(
                    Mode.NUMERIC,
                    from,
                    0,
                    if (from + 1 >= inputLength ||
                        !canEncode(Mode.NUMERIC, stringToEncode.get(from + 1))
                    ) {
                        1
                    } else if (from + 2 >= inputLength ||
                        !canEncode(Mode.NUMERIC, stringToEncode.get(from + 2))
                    ) {
                        2
                    } else {
                        3
                    },
                    previous,
                    version,
                ),
            )
        }
    }

    @Throws(WriterException::class)
    private fun encodeSpecificVersion(version: Version): ResultList {
        val inputLength/* A vertex represents a tuple of a position in the input, a mode and a character encoding where position 0
         * denotes the position left of the first character, 1 the position left of the second character and so on.
         * Likewise the end vertices are located after the last character at position stringToEncode.length().
         *
         * An edge leading to such a vertex encodes one or more of the characters left of the position that the vertex
         * represents and encodes it in the same encoding and mode as the vertex on which the edge ends. In other words,
         * all edges leading to a particular vertex encode the same characters in the same mode with the same character
         * encoding. They differ only by their source vertices who are all located at i+1 minus the number of encoded
         * characters.
         *
         * The edges leading to a vertex are stored in such a way that there is a fast way to enumerate the edges ending
         * on a particular vertex.
         *
         * The algorithm processes the vertices in order of their position thereby performing the following:
         *
         * For every vertex at position i the algorithm enumerates the edges ending on the vertex and removes all but the
         * shortest from that list.
         * Then it processes the vertices for the position i+1. If i+1 == stringToEncode.length() then the algorithm ends
         * and chooses the the edge with the smallest size from any of the edges leading to vertices at this position.
         * Otherwise the algorithm computes all possible outgoing edges for the vertices at the position i+1
         *
         * Examples:
         * The process is illustrated by showing the graph (edges) after each iteration from left to right over the input:
         * An edge is drawn as follows "(" + fromVertex + ") -- " + encodingMode + "(" + encodedInput + ") (" +
         * accumulatedSize + ") --> (" + toVertex + ")"
         *
         * Example 1 encoding the string "ABCDE":
         * Note: This example assumes that alphanumeric encoding is only possible in multiples of two characters so that
         * the example is both short and showing the principle. In reality this restriction does not exist.
         *
         * Initial situation
         * (initial) -- BYTE(A) (20) --> (1_BYTE)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
         *
         * Situation after adding edges to vertices at position 1
         * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
         *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
         *
         * Situation after adding edges to vertices at position 2
         * (initial) -- BYTE(A) (20) --> (1_BYTE)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
         * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
         * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- BYTE(C) (44) --> (3_BYTE)
         *                                                            (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
         *
         * Situation after adding edges to vertices at position 3
         * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE)
         *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- BYTE(D) (64) --> (4_BYTE)
         *                                                                                                 (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
         *                                                            (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
         *
         * Situation after adding edges to vertices at position 4
         * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D) (44) --> (4_BYTE)
         *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC) -- BYTE(E) (55) --> (5_BYTE)
         *
         * Situation after adding edges to vertices at position 5
         * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D)         (44) --> (4_BYTE) -- BYTE(E)         (52) --> (5_BYTE)
         *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
         * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
         *
         * Encoding as BYTE(ABCDE) has the smallest size of 52 and is hence chosen. The encodation ALPHANUMERIC(ABCD),
         * BYTE(E) is longer with a size of 55.
         *
         * Example 2 encoding the string "XXYY" where X denotes a character unique to character set ISO-8859-2 and Y a
         * character unique to ISO-8859-3. Both characters encode as double byte in UTF-8:
         *
         * Initial situation
         * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
         *
         * Situation after adding edges to vertices at position 1
         * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
         *                               (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-8)
         *                               (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-16BE)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
         *
         * Situation after adding edges to vertices at position 2
         * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
         *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
         *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-8)
         *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-16BE)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE)
         *
         * Situation after adding edges to vertices at position 3
         * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
         *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
         *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-8)
         *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-16BE)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
         *
         * Situation after adding edges to vertices at position 4
         * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
         *                                                                                                               (3_BYTE_UTF-8) -- BYTE(Y) (88) --> (4_BYTE_UTF-8)
         *                                                                                                               (3_BYTE_UTF-16BE) -- BYTE(Y) (88) --> (4_BYTE_UTF-16BE)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
         * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
         *
         * Encoding as ECI(ISO-8859-2),BYTE(XX),ECI(ISO-8859-3),BYTE(YY) has the smallest size of 80 and is hence chosen.
         * The encodation ECI(UTF-8),BYTE(XXYY) is longer with a size of 88.
         */ =
            stringToEncode.length

        // Array that represents vertices. There is a vertex for every character, encoding and mode. The vertex contains
        // a list of all edges that lead to it that have the same encoding and mode.
        // The lists are created lazily

        // The last dimension in the array below encodes the 4 modes KANJI, ALPHANUMERIC, NUMERIC and BYTE via the
        // function getCompactedOrdinal(Mode)
        val edges =
            Array(inputLength + 1) { Array(encoders.length()) { arrayOfNulls<Edge>(4) } }
        addEdges(version, edges, 0, null)

        for (i in 1..inputLength) {
            for (j in 0..<encoders.length()) {
                for (k in 0..3) {
                    if (edges[i][j][k] != null && i < inputLength) {
                        addEdges(version, edges, i, edges[i][j][k])
                    }
                }
            }
        }
        var minimalJ = -1
        var minimalK = -1
        var minimalSize = Int.MAX_VALUE
        for (j in 0..<encoders.length()) {
            for (k in 0..3) {
                val edge = edges[inputLength][j][k]
                if (edge != null) {
                    if (edge.cachedTotalSize < minimalSize) {
                        minimalSize = edge.cachedTotalSize
                        minimalJ = j
                        minimalK = k
                    }
                }
            }
        }
        if (minimalJ < 0) {
            throw WriterException("Internal error: failed to encode \"" + stringToEncode + "\"")
        }
        return ResultList(version, edges[inputLength][minimalJ][minimalK])
    }

    internal inner class Edge(
        val mode: Mode,
        val fromPosition: Int,
        charsetEncoderIndex: Int,
        val characterLength: Int,
        val previous: Edge?,
        version: Version,
    ) {
        val charsetEncoderIndex: Int
        val cachedTotalSize: Int

        init {
            this.charsetEncoderIndex =
                if (mode == Mode.BYTE || previous == null) charsetEncoderIndex else previous.charsetEncoderIndex // inherit the encoding if not of type BYTE

            var size = if (previous != null) previous.cachedTotalSize else 0

            val needECI =
                mode == Mode.BYTE &&
                    (previous == null && this.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
                    (previous != null && this.charsetEncoderIndex != previous.charsetEncoderIndex)

            if (previous == null || mode != previous.mode || needECI) {
                size += 4 + mode.getCharacterCountBits(version)
            }
            when (mode) {
                Mode.KANJI -> {
                    size += 13
                }

                Mode.ALPHANUMERIC -> {
                    size += if (characterLength == 1) 6 else 11
                }

                Mode.NUMERIC -> {
                    size +=
                        if (characterLength == 1) {
                            4
                        } else if (characterLength == 2) {
                            7
                        } else {
                            10
                        }
                }

                Mode.BYTE -> {
                    size += 8 *
                        encoders
                            .encode(
                                stringToEncode.substring(fromPosition, fromPosition + characterLength),
                                charsetEncoderIndex,
                            ).size
                    if (needECI) {
                        size += 4 + 8 // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
                    }
                }

                else -> {
                    error("Unsupported compact mode: $mode")
                }
            }
            cachedTotalSize = size
        }
    }

    internal inner class ResultList(
        version: Version,
        solution: Edge?,
    ) {
        private val list: MutableList<ResultNode> = ArrayList<ResultNode>()
        val version: Version

        init {
            var length = 0
            var current = solution
            var containsECI = false

            while (current != null) {
                length += current.characterLength
                val previous = current.previous

                val needECI =
                    current.mode == Mode.BYTE &&
                        (previous == null && current.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
                        (previous != null && current.charsetEncoderIndex != previous.charsetEncoderIndex)

                if (needECI) {
                    containsECI = true
                }

                if (previous == null || previous.mode != current.mode || needECI) {
                    list.add(0, ResultNode(current.mode, current.fromPosition, current.charsetEncoderIndex, length))
                    length = 0
                }

                if (needECI) {
                    list.add(0, ResultNode(Mode.ECI, current.fromPosition, current.charsetEncoderIndex, 0))
                }
                current = previous
            }

            // prepend FNC1 if needed. If the bits contain an ECI then the FNC1 must be preceeded by an ECI.
            // If there is no ECI at the beginning then we put an ECI to the default charset (ISO-8859-1)
            if (isGS1) {
                var first = list.get(0)
                if (first != null && first.mode != Mode.ECI && containsECI) {
                    // prepend a default character set ECI
                    list.add(0, ResultNode(Mode.ECI, 0, 0, 0))
                }
                first = list.get(0)
                // prepend or insert a FNC1_FIRST_POSITION after the ECI (if any)
                list.add(if (first.mode != Mode.ECI) 0 else 1, ResultNode(Mode.FNC1_FIRST_POSITION, 0, 0, 0))
            }

            // set version to smallest version into which the bits fit.
            var versionNumber = version.versionNumber
            val lowerLimit: Int
            val upperLimit: Int
            when (getVersionSize(version)) {
                VersionSize.SMALL -> {
                    lowerLimit = 1
                    upperLimit = 9
                }

                VersionSize.MEDIUM -> {
                    lowerLimit = 10
                    upperLimit = 26
                }

                VersionSize.LARGE -> {
                    lowerLimit = 27
                    upperLimit = 40
                }
            }
            val size = getSize(version)
            // increase version if needed
            while (versionNumber < upperLimit &&
                !Encoder.willFit(
                    size,
                    getVersionForNumber(versionNumber),
                    ecLevel,
                )
            ) {
                versionNumber++
            }
            // shrink version if possible
            while (versionNumber > lowerLimit &&
                Encoder.willFit(
                    size,
                    getVersionForNumber(versionNumber - 1),
                    ecLevel,
                )
            ) {
                versionNumber--
            }
            this.version = getVersionForNumber(versionNumber)
        }

        val size: Int
            /**
             * returns the size in bits
             */
            get() = getSize(version)

        private fun getSize(version: Version): Int {
            var result = 0
            for (resultNode in list) {
                result += resultNode.getSize(version)
            }
            return result
        }

        /**
         * appends the bits
         */
        @Throws(WriterException::class)
        fun getBits(bits: BitArray) {
            for (resultNode in list) {
                resultNode.getBits(bits)
            }
        }

        override fun toString(): String {
            val result = StringBuilder()
            var previous: ResultNode? = null
            for (current in list) {
                if (previous != null) {
                    result.append(",")
                }
                result.append(current.toString())
                previous = current
            }
            return result.toString()
        }

        internal inner class ResultNode(
            val mode: Mode,
            private val fromPosition: Int,
            private val charsetEncoderIndex: Int,
            private val characterLength: Int,
        ) {
            /**
             * returns the size in bits
             */
            fun getSize(version: Version): Int {
                var size = 4 + mode.getCharacterCountBits(version)
                when (mode) {
                    Mode.KANJI -> {
                        size += 13 * characterLength
                    }

                    Mode.ALPHANUMERIC -> {
                        size += (characterLength / 2) * 11
                        size += if ((characterLength % 2) == 1) 6 else 0
                    }

                    Mode.NUMERIC -> {
                        size += (characterLength / 3) * 10
                        val rest = characterLength % 3
                        size +=
                            if (rest == 1) {
                                4
                            } else if (rest == 2) {
                                7
                            } else {
                                0
                            }
                    }

                    Mode.BYTE -> {
                        size += 8 * this.characterCountIndicator
                    }

                    Mode.ECI -> {
                        size += 8
                    }

                    // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
                    else -> {
                        Unit
                    }
                }
                return size
            }

            private val characterCountIndicator: Int
                /**
                 * returns the length in characters according to the specification (differs from getCharacterLength() in BYTE mode
                 * for multi byte encoded characters)
                 */
                get() =
                    if (mode == Mode.BYTE) {
                        encoders
                            .encode(
                                stringToEncode.substring(fromPosition, fromPosition + characterLength),
                                charsetEncoderIndex,
                            ).size
                    } else {
                        characterLength
                    }

            /**
             * appends the bits
             */
            @Throws(WriterException::class)
            fun getBits(bits: BitArray) {
                bits.appendBits(mode.bits, 4)
                if (characterLength > 0) {
                    val length = this.characterCountIndicator
                    bits.appendBits(length, mode.getCharacterCountBits(version))
                }
                if (mode == Mode.ECI) {
                    bits.appendBits(encoders.getECIValue(charsetEncoderIndex), 8)
                } else if (characterLength > 0) {
                    // append data
                    Encoder.appendBytes(
                        stringToEncode.substring(fromPosition, fromPosition + characterLength),
                        mode,
                        bits,
                        encoders.getCharset(charsetEncoderIndex),
                    )
                }
            }

            override fun toString(): String {
                val result = StringBuilder()
                result.append(mode).append('(')
                if (mode == Mode.ECI) {
                    result.append(encoders.getCharset(charsetEncoderIndex).displayName())
                } else {
                    result.append(makePrintable(stringToEncode.substring(fromPosition, fromPosition + characterLength)))
                }
                result.append(')')
                return result.toString()
            }

            private fun makePrintable(s: String): String {
                val result = StringBuilder()
                for (i in 0..<s.length) {
                    if (s.get(i).code < 32 || s.get(i).code > 126) {
                        result.append('.')
                    } else {
                        result.append(s.get(i))
                    }
                }
                return result.toString()
            }
        }
    }

    companion object {
        /**
         * Encodes the string minimally
         *
         * @param stringToEncode The string to encode
         * @param version The preferred [Version]. A minimal version is computed (see
         * [method][ResultList.getVersion] when the value of the argument is null
         * @param priorityCharset The preferred [Charset]. When the value of the argument is null, the algorithm
         * chooses charsets that leads to a minimal representation. Otherwise the algorithm will use the priority
         * charset to encode any character in the input that can be encoded by it if the charset is among the
         * supported charsets.
         * @param isGS1 `true` if a FNC1 is to be prepended; `false` otherwise
         * @param ecLevel The error correction level.
         * @return An instance of `ResultList` representing the minimal solution.
         * @see ResultList.getBits
         *
         * @see ResultList.getVersion
         *
         * @see ResultList.getSize
         */
        @Throws(WriterException::class)
        fun encode(
            stringToEncode: String,
            version: Version?,
            priorityCharset: Charset?,
            isGS1: Boolean,
            ecLevel: ErrorCorrectionLevel,
        ): ResultList = MinimalEncoder(stringToEncode, priorityCharset, isGS1, ecLevel).encode(version)

        private fun getVersionSize(version: Version): VersionSize =
            if (version.versionNumber <=
                9
            ) {
                VersionSize.SMALL
            } else if (version.versionNumber <= 26) {
                VersionSize.MEDIUM
            } else {
                VersionSize.LARGE
            }

        private fun getVersion(versionSize: VersionSize): Version =
            when (versionSize) {
                VersionSize.SMALL -> getVersionForNumber(9)
                VersionSize.MEDIUM -> getVersionForNumber(26)
                VersionSize.LARGE -> getVersionForNumber(40)
            }

        fun isNumeric(c: Char): Boolean = c >= '0' && c <= '9'

        fun isDoubleByteKanji(c: Char): Boolean = Encoder.isOnlyDoubleByteKanji(c.toString())

        fun isAlphanumeric(c: Char): Boolean = Encoder.getAlphanumericCode(c.code) != -1

        fun getCompactedOrdinal(mode: Mode?): Int {
            if (mode == null) {
                return 0
            }
            when (mode) {
                Mode.KANJI -> return 0
                Mode.ALPHANUMERIC -> return 1
                Mode.NUMERIC -> return 2
                Mode.BYTE -> return 3
                else -> throw IllegalStateException("Illegal mode " + mode)
            }
        }
    }
}
