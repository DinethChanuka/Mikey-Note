package com.wordbook.core.domain.exception;

/**
 * Thrown when an underlying database or I/O failure occurs in persistence mechanisms.
 */
public class PersistenceException extends WordBookException {
    public PersistenceException(String message) {
        super(message, 500);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause, 500);
    }
}
