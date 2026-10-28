package com.wordbook.core.domain.exception;

/**
 * Thrown when an entity (Book, Lesson, etc.) cannot be located by its identifier.
 */
public class EntityNotFoundException extends WordBookException {
    public EntityNotFoundException(String entityType, String id) {
        super(entityType + " with id '" + id + "' was not found.", 404);
    }
}
