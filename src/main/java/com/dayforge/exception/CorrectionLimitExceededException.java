package com.dayforge.exception;

public class CorrectionLimitExceededException extends RuntimeException {
    public CorrectionLimitExceededException(String message) {
        super(message);
    }
}

