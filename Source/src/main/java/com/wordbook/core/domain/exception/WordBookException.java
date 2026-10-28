package com.wordbook.core.domain.exception;

/**
 * Base exception for all WordBook domain and application errors.
 */
public class WordBookException extends RuntimeException {
    private final int statusCode;

    public WordBookException(String message) {
        this(message, 500);
    }

    public WordBookException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public WordBookException(String message, Throwable cause) {
        this(message, cause, 500);
    }

    public WordBookException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
