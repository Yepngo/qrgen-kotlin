package com.yepngo.qrgen.internal.zxing;

import com.yepngo.qrgen.internal.zxing.common.BitMatrix;
import com.yepngo.qrgen.internal.zxing.qrcode.QRCodeWriter;
import com.yepngo.qrgen.internal.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrEncoderParityTest {
    private static Stream<Arguments> zxing353GoldenMatrices() {
        return Stream.of(
                Arguments.of(
                        "0123456789012345678901234567890123456789",
                        ErrorCorrectionLevel.L,
                        StandardCharsets.UTF_8,
                        21,
                        "3e3580e4c1b025f8d6e8cf350313a113dbd27b5b896b2a6a4770688f2212553a"),
                Arguments.of(
                        "HELLO WORLD 1234567890 $%*+-./:",
                        ErrorCorrectionLevel.M,
                        StandardCharsets.UTF_8,
                        25,
                        "b623a064d9b5159a67fd8879d91cc36b67134b1fb23e69c5f4ccdebcd5fdf50b"),
                Arguments.of(
                        "こんにちは世界 / café / QR",
                        ErrorCorrectionLevel.Q,
                        StandardCharsets.UTF_8,
                        33,
                        "6a04c46c120044b9ec7d8a361f5a7c531d7d4919a4bb3d0e1a9adba51fd4429c"),
                Arguments.of(
                        "https://get.yepngo.com/referrals/42?source=qrgen",
                        ErrorCorrectionLevel.H,
                        StandardCharsets.UTF_8,
                        41,
                        "3f34ecafbd3650e4bdc9554db550e2c93f68ede98fd1e619eb45454d134100d5"),
                Arguments.of(
                        "Café déjà vu",
                        ErrorCorrectionLevel.M,
                        StandardCharsets.ISO_8859_1,
                        21,
                        "3f0469f1b560c65e4cce49daf3fd85a4e853e366dc46b48626961588ab06021e"),
                Arguments.of(
                        repeat("0123456789", 250),
                        ErrorCorrectionLevel.Q,
                        StandardCharsets.UTF_8,
                        145,
                        "b08a1879121ebb06b98cf2cac94d925be49ae31d43091cde72c733ecf5576b69")
        );
    }

    @ParameterizedTest
    @MethodSource("zxing353GoldenMatrices")
    void matchesZxing353Matrix(
            String payload,
            ErrorCorrectionLevel level,
            Charset charset,
            int expectedWidth,
            String expectedFingerprint) throws WriterException, NoSuchAlgorithmException {
        final Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, level);
        hints.put(EncodeHintType.CHARACTER_SET, charset.name());
        hints.put(EncodeHintType.MARGIN, 0);

        final BitMatrix matrix = new QRCodeWriter().encode(payload, 1, 1, hints);

        assertEquals(expectedWidth, matrix.getWidth());
        assertEquals(expectedWidth, matrix.getHeight());
        assertEquals(expectedFingerprint, fingerprint(matrix));
    }

    private static String fingerprint(BitMatrix matrix) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((matrix.getWidth() + "x" + matrix.getHeight() + "\n")
                .getBytes(StandardCharsets.US_ASCII));
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                digest.update((byte) (matrix.get(x, y) ? 1 : 0));
            }
        }

        final StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static String repeat(String value, int count) {
        final StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
