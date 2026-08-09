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
package com.yepngo.qrgen.internal.zxing.common

import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException

/**
 * Set of CharsetEncoders for a given input string
 *
 * Invariants:
 * - The list contains only encoders from CharacterSetECI (list is shorter then the list of encoders available on
 * the platform for which ECI values are defined).
 * - The list contains encoders at least one encoder for every character in the input.
 * - The first encoder in the list is always the ISO-8859-1 encoder even of no character in the input can be encoded
 * by it.
 * - If the input contains a character that is not in ISO-8859-1 then the last two entries in the list will be the
 * UTF-8 encoder and the UTF-16BE encoder.
 *
 * @author Alex Geller
 */
internal class ECIEncoderSet(
    stringToEncode: String,
    priorityCharset: Charset?,
    fnc1: Int,
) {
    private val encoders: Array<CharsetEncoder>

    /*
     *  returns -1 if no priority charset was defined
     */
    val priorityEncoderIndex: Int

    /**
     * Constructs an encoder set
     *
     * @param stringToEncode the string that needs to be encoded
     * @param priorityCharset The preferred [Charset] or null.
     * @param fnc1 fnc1 denotes the character in the input that represents the FNC1 character or -1 for a non-GS1 bar
     * code. When specified, it is considered an error to pass it as argument to the methods canEncode() or encode().
     */
    init {
        val neededEncoders: MutableList<CharsetEncoder> = ArrayList<CharsetEncoder>()

        // we always need the ISO-8859-1 encoder. It is the default encoding
        neededEncoders.add(StandardCharsets.ISO_8859_1.newEncoder())
        var needUnicodeEncoder = priorityCharset != null && priorityCharset.name().startsWith("UTF")

        // Walk over the input string and see if all characters can be encoded with the list of encoders
        for (i in 0..<stringToEncode.length) {
            var canEncode = false
            for (encoder in neededEncoders) {
                val c = stringToEncode.get(i)
                if (c.code == fnc1 || encoder.canEncode(c)) {
                    canEncode = true
                    break
                }
            }

            if (!canEncode) {
                // for the character at position i we don't yet have an encoder in the list
                for (encoder in ENCODERS) {
                    if (encoder.canEncode(stringToEncode.get(i))) {
                        // Good, we found an encoder that can encode the character. We add him to the list and continue scanning
                        // the input
                        neededEncoders.add(encoder)
                        canEncode = true
                        break
                    }
                }
            }

            if (!canEncode) {
                // The character is not encodeable by any of the single byte encoders so we remember that we will need a
                // Unicode encoder.
                needUnicodeEncoder = true
            }
        }

        if (neededEncoders.size == 1 && !needUnicodeEncoder) {
            // the entire input can be encoded by the ISO-8859-1 encoder
            encoders = arrayOf<CharsetEncoder>(neededEncoders.get(0))
        } else {
            // we need more than one single byte encoder or we need a Unicode encoder.
            // In this case we append a UTF-8 and UTF-16 encoder to the list
            val collected = arrayOfNulls<CharsetEncoder>(neededEncoders.size + 2)
            var index = 0
            for (encoder in neededEncoders) {
                collected[index++] = encoder
            }

            collected[index] = StandardCharsets.UTF_8.newEncoder()
            collected[index + 1] = StandardCharsets.UTF_16BE.newEncoder()
            encoders = Array(collected.size) { checkNotNull(collected[it]) }
        }

        // Compute priorityEncoderIndex by looking up priorityCharset in encoders
        var priorityEncoderIndexValue = -1
        if (priorityCharset != null) {
            for (i in encoders.indices) {
                if (encoders[i] != null && priorityCharset.name() == encoders[i].charset().name()) {
                    priorityEncoderIndexValue = i
                    break
                }
            }
        }
        priorityEncoderIndex = priorityEncoderIndexValue
        // invariants
        assert(encoders[0].charset() == StandardCharsets.ISO_8859_1)
    }

    fun length(): Int = encoders.size

    fun getCharsetName(index: Int): String {
        assert(index < length())
        return encoders[index].charset().name()
    }

    fun getCharset(index: Int): Charset {
        assert(index < length())
        return encoders[index].charset()
    }

    fun getECIValue(encoderIndex: Int): Int = checkNotNull(CharacterSetECI.getCharacterSetECI(encoders[encoderIndex].charset())).value

    fun canEncode(
        c: Char,
        encoderIndex: Int,
    ): Boolean {
        assert(encoderIndex < length())
        val encoder = encoders[encoderIndex]
        return encoder.canEncode("" + c)
    }

    fun encode(
        c: Char,
        encoderIndex: Int,
    ): ByteArray {
        assert(encoderIndex < length())
        val encoder = encoders[encoderIndex]
        assert(encoder.canEncode("" + c))
        return ("" + c).toByteArray(encoder.charset())
    }

    fun encode(
        s: String,
        encoderIndex: Int,
    ): ByteArray {
        assert(encoderIndex < length())
        val encoder = encoders[encoderIndex]
        return s.toByteArray(encoder.charset())
    }

    companion object {
        // List of encoders that potentially encode characters not in ISO-8859-1 in one byte.
        private val ENCODERS: MutableList<CharsetEncoder> = ArrayList<CharsetEncoder>()

        init {
            val names =
                arrayOf<String?>(
                    "IBM437",
                    "ISO-8859-2",
                    "ISO-8859-3",
                    "ISO-8859-4",
                    "ISO-8859-5",
                    "ISO-8859-6",
                    "ISO-8859-7",
                    "ISO-8859-8",
                    "ISO-8859-9",
                    "ISO-8859-10",
                    "ISO-8859-11",
                    "ISO-8859-13",
                    "ISO-8859-14",
                    "ISO-8859-15",
                    "ISO-8859-16",
                    "windows-1250",
                    "windows-1251",
                    "windows-1252",
                    "windows-1256",
                    "Shift_JIS",
                )
            for (name in names) {
                if (CharacterSetECI.getCharacterSetECIByName(name) != null) {
                    try {
                        ENCODERS.add(Charset.forName(name).newEncoder())
                    } catch (e: UnsupportedCharsetException) {
                        // continue
                    }
                }
            }
        }
    }
}
