package com.wordbook.core.domain.repository;

import java.util.function.Supplier;

/**
 * Port interface for coordinating database transactions across multiple repositories.
 */
public interface UnitOfWork {
    void executeInTransaction(Runnable action);
    <T> T executeInTransaction(Supplier<T> action);
}
