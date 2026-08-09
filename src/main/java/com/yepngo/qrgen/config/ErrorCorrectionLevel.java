package com.yepngo.qrgen.config;

/**
 * The error correction level for a QR code
 * <p>
 * The specification defines four values for error correction level.
 * The levels describe a level of redundancy of the encoded information.
 * Higher redundancy causes bigger QR codes but also better resilience
 * against (physical) damage or occlusion of the displayed QR code
 */
public enum ErrorCorrectionLevel {
    /**
     * L = ~7% correction
     */
    L,

    /**
     * M = ~15% correction
     */
    M,

    /**
     * Q = ~25% correction
     */
    Q,

    /**
     * H = ~30% correction
     */
    H
}
