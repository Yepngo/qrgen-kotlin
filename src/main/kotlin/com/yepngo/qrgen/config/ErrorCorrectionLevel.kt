package com.yepngo.qrgen.config

/**
 * The error correction level for a QR code
 *
 *
 * The specification defines four values for error correction level.
 * The levels describe a level of redundancy of the encoded information.
 * Higher redundancy causes bigger QR codes but also better resilience
 * against (physical) damage or occlusion of the displayed QR code
 */
public enum class ErrorCorrectionLevel(
    public val maximumPayloadBytes: Int,
) {
    /**
     * L = ~7% correction
     */
    L(2_953),

    /**
     * M = ~15% correction
     */
    M(2_331),

    /**
     * Q = ~25% correction
     */
    Q(1_663),

    /**
     * H = ~30% correction
     */
    H(1_273),
}
