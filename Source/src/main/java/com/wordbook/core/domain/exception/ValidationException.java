package com.wordbook.core.domain.exception;

/**
 * Thrown when domain validation constraints are violated (e.g. empty title, invalid color).
 */
public class ValidationException extends WordBookException {
    public ValidationException(String message) {
        super(message, 400);
    }
}
