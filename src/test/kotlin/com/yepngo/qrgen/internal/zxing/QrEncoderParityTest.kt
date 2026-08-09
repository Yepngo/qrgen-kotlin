package com.yepngo.qrgen.internal.zxing

import com.yepngo.qrgen.internal.zxing.common.BitMatrix
import com.yepngo.qrgen.internal.zxing.qrcode.QRCodeWriter
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.stream.Stream

internal class QrEncoderParityTest {
    @ParameterizedTest
    @MethodSource("zxing353GoldenMatrices")
    @kotlin.Throws(WriterException::class, NoSuchAlgorithmException::class)
    fun matchesZxing353Matrix(
        payload: String,
        level: ErrorCorrectionLevel,
        charset: Charset,
        expectedWidth: Int,
        expectedFingerprint: String,
    ) {
        val hints: MutableMap<EncodeHintType?, Any?> = hashMapOf()
        hints.put(EncodeHintType.ERROR_CORRECTION, level)
        hints.put(EncodeHintType.CHARACTER_SET, charset.name())
        hints.put(EncodeHintType.MARGIN, 0)

        val matrix = QRCodeWriter().encode(payload, 1, 1, hints)

        Assertions.assertEquals(expectedWidth, matrix.width)
        Assertions.assertEquals(expectedWidth, matrix.height)
        Assertions.assertEquals(expectedFingerprint, fingerprint(matrix))
    }

    companion object {
        @JvmStatic
        private fun zxing353GoldenMatrices(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "0123456789012345678901234567890123456789",
                    ErrorCorrectionLevel.L,
                    StandardCharsets.UTF_8,
                    21,
                    "3e3580e4c1b025f8d6e8cf350313a113dbd27b5b896b2a6a4770688f2212553a",
                ),
                Arguments.of(
                    "HELLO WORLD 1234567890 $%*+-./:",
                    ErrorCorrectionLevel.M,
                    StandardCharsets.UTF_8,
                    25,
                    "b623a064d9b5159a67fd8879d91cc36b67134b1fb23e69c5f4ccdebcd5fdf50b",
                ),
                Arguments.of(
                    "こんにちは世界 / café / QR",
                    ErrorCorrectionLevel.Q,
                    StandardCharsets.UTF_8,
                    33,
                    "6a04c46c120044b9ec7d8a361f5a7c531d7d4919a4bb3d0e1a9adba51fd4429c",
                ),
                Arguments.of(
                    "https://get.yepngo.com/referrals/42?source=qrgen",
                    ErrorCorrectionLevel.H,
                    StandardCharsets.UTF_8,
                    41,
                    "3f34ecafbd3650e4bdc9554db550e2c93f68ede98fd1e619eb45454d134100d5",
                ),
                Arguments.of(
                    "Café déjà vu",
                    ErrorCorrectionLevel.M,
                    StandardCharsets.ISO_8859_1,
                    21,
                    "3f0469f1b560c65e4cce49daf3fd85a4e853e366dc46b48626961588ab06021e",
                ),
                Arguments.of(
                    repeat("0123456789", 250),
                    ErrorCorrectionLevel.Q,
                    StandardCharsets.UTF_8,
                    145,
                    "b08a1879121ebb06b98cf2cac94d925be49ae31d43091cde72c733ecf5576b69",
                ),
            )

        @kotlin.Throws(NoSuchAlgorithmException::class)
        private fun fingerprint(matrix: BitMatrix): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(
                (matrix.width.toString() + "x" + matrix.height + "\n")
                    .toByteArray(StandardCharsets.US_ASCII),
            )
            for (y in 0..<matrix.height) {
                for (x in 0..<matrix.width) {
                    digest.update((if (matrix.get(x, y)) 1 else 0).toByte())
                }
            }

            val result = StringBuilder()
            for (value in digest.digest()) {
                result.append(String.format("%02x", value.toInt() and 0xff))
            }
            return result.toString()
        }

        private fun repeat(
            value: String,
            count: Int,
        ): String {
            val result = StringBuilder(value.length * count)
            for (i in 0..<count) {
                result.append(value)
            }
            return result.toString()
        }
    }
}
