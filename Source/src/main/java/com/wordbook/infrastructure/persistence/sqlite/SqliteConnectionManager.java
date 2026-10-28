package com.wordbook.infrastructure.persistence.sqlite;

import com.wordbook.core.domain.exception.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * High-performance SQLite Connection Manager supporting transactional thread-local contexts,
 * Write-Ahead Logging (WAL) concurrency, foreign key enforcement, and configurable timeouts.
 */
public class SqliteConnectionManager implements AutoCloseable {
    private final String jdbcUrl;
    private final String dbPath;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private Connection keepAliveConnection;

    public SqliteConnectionManager(String dbFilePath) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new PersistenceException("SQLite JDBC driver not found on classpath", e);
        }

        if (dbFilePath == null || dbFilePath.trim().isEmpty() || ":memory:".equalsIgnoreCase(dbFilePath.trim())) {
            this.dbPath = ":memory:";
            this.jdbcUrl = "jdbc:sqlite:file:" + java.util.UUID.randomUUID().toString().substring(0, 8) + "?mode=memory&cache=shared";
            try {
                this.keepAliveConnection = DriverManager.getConnection(this.jdbcUrl);
                configurePragmas(this.keepAliveConnection);
            } catch (SQLException e) {
                throw new PersistenceException("Failed to initialize in-memory SQLite database", e);
            }
        } else {
            this.dbPath = dbFilePath.trim();
            this.jdbcUrl = "jdbc:sqlite:" + this.dbPath;
        }
    }

    public Connection getConnection() throws SQLException {
        Connection txConn = transactionConnection.get();
        if (txConn != null && !txConn.isClosed()) {
            return txConn;
        }

        Connection conn = DriverManager.getConnection(jdbcUrl);
        configurePragmas(conn);
        return conn;
    }

    public void startTransaction() throws SQLException {
        if (transactionConnection.get() != null) {
            throw new PersistenceException("A transaction is already active on this thread.");
        }
        Connection conn = DriverManager.getConnection(jdbcUrl);
        configurePragmas(conn);
        conn.setAutoCommit(false);
        transactionConnection.set(conn);
    }

    public void commitTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn == null) {
            throw new PersistenceException("No active transaction to commit.");
        }
        try {
            conn.commit();
        } finally {
            closeTransactionConnection(conn);
        }
    }

    public void rollbackTransaction() {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                // Log and discard during rollback
            } finally {
                closeTransactionConnection(conn);
            }
        }
    }

    public boolean isInTransaction() {
        return transactionConnection.get() != null;
    }

    private void closeTransactionConnection(Connection conn) {
        transactionConnection.remove();
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {}
        }
    }

    public void releaseConnection(Connection conn) {
        if (conn == null) return;
        // If it's the current transaction connection, do not close it yet
        if (conn == transactionConnection.get()) {
            return;
        }
        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {}
    }

    private void configurePragmas(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            if (!":memory:".equals(dbPath)) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
    }

    public String getDbPath() {
        return dbPath;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public void close() {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            closeTransactionConnection(conn);
        }
        if (keepAliveConnection != null) {
            try {
                if (!keepAliveConnection.isClosed()) {
                    keepAliveConnection.close();
                }
            } catch (SQLException ignored) {}
            keepAliveConnection = null;
        }
    }
}
