package com.wordbook.infrastructure.persistence.sqlite;

import com.wordbook.core.domain.exception.PersistenceException;
import com.wordbook.core.domain.repository.UnitOfWork;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * SQLite implementation of UnitOfWork for atomicity and isolation.
 */
public class SqliteUnitOfWork implements UnitOfWork {
    private final SqliteConnectionManager connectionManager;

    public SqliteUnitOfWork(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void executeInTransaction(Runnable action) {
        executeInTransaction(() -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> action) {
        if (connectionManager.isInTransaction()) {
            return action.get();
        }

        try {
            connectionManager.startTransaction();
            T result = action.get();
            connectionManager.commitTransaction();
            return result;
        } catch (Exception e) {
            connectionManager.rollbackTransaction();
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new PersistenceException("Transaction failed: " + e.getMessage(), e);
        }
    }
}
