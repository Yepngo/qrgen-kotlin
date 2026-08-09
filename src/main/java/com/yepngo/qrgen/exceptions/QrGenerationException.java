package com.yepngo.qrgen.exceptions;

public class QrGenerationException extends Exception {
    public QrGenerationException(String message) {
        super(message);
    }

    public QrGenerationException(String message, Exception e) {
        super(message, e);
    }
}
