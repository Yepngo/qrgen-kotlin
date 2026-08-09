package com.yepngo.qrgen

import java.nio.charset.StandardCharsets

object Payload {
    private const val COUNT_50 = "012345678901234567890123456789012345678901234567890123456789"
    private val count200 = COUNT_50 + COUNT_50 + COUNT_50 + COUNT_50
    private val count1000 = count200 + count200 + count200 + count200 + count200
    private val count5000 = count1000 + count1000 + count1000 + count1000 + count1000
    private val count10000 = count5000 + count5000
    private val raw10000: ByteArray = count10000.toByteArray(StandardCharsets.UTF_8)

    fun getWithLength(length: Int): String {
        if (length > raw10000.size) throw RuntimeException("requested paylaod size too long")

        return String(raw10000, 0, length, StandardCharsets.UTF_8)
    }
}
