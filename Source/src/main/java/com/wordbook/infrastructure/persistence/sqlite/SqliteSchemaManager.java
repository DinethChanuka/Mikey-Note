package com.wordbook.infrastructure.persistence.sqlite;

import com.wordbook.core.domain.exception.PersistenceException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes automated database schema creation and versioned migrations.
 */
public class SqliteSchemaManager {
    private final SqliteConnectionManager connectionManager;

    public SqliteSchemaManager(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void initializeSchema() {
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            conn.setAutoCommit(false);

            ensureVersionTable(conn);
            int currentVersion = getCurrentVersion(conn);

            if (currentVersion < 1) {
                applyMigrationV1(conn);
                recordMigration(conn, 1, "Initial schema: books and lessons tables with cascading FK");
            }

            if (currentVersion < 2) {
                applyMigrationV2(conn);
                recordMigration(conn, 2, "Performance indexes and query optimizations");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new PersistenceException("Failed to initialize SQLite schema: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                connectionManager.releaseConnection(conn);
            }
        }
    }

    private void ensureVersionTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at INTEGER NOT NULL
                );
            """);
        }
    }

    private int getCurrentVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private void recordMigration(Connection conn, int version, String description) throws SQLException {
        String sql = String.format(
                "INSERT INTO schema_version (version, description, applied_at) VALUES (%d, '%s', %d)",
                version, description.replace("'", "''"), System.currentTimeMillis()
        );
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void applyMigrationV1(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    color TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lessons (
                    id TEXT PRIMARY KEY,
                    book_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    content_html TEXT NOT NULL,
                    word_count INTEGER NOT NULL DEFAULT 0,
                    char_count INTEGER NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
                );
            """);
        }
    }

    private void applyMigrationV2(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lessons_book_id ON lessons(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lessons_sort ON lessons(book_id, sort_order ASC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_updated ON books(updated_at DESC);");
        }
    }
}
