package com.wordbook.infrastructure.persistence.sqlite;

import com.wordbook.core.domain.exception.PersistenceException;
import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.repository.BookRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite JDBC implementation of BookRepository using PreparedStatements.
 */
public class SqliteBookRepository implements BookRepository {
    private final SqliteConnectionManager connectionManager;

    public SqliteBookRepository(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void save(Book book) {
        String sql = "INSERT INTO books (id, title, color, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, book.getId());
                ps.setString(2, book.getTitle());
                ps.setString(3, book.getColor());
                ps.setLong(4, book.getCreatedAt());
                ps.setLong(5, book.getUpdatedAt());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save book with id " + book.getId(), e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Book> findById(String id) {
        String sql = "SELECT id, title, color, created_at, updated_at FROM books WHERE id = ?";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToBook(rs));
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find book by id " + id, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public List<Book> findAll() {
        String sql = "SELECT id, title, color, created_at, updated_at FROM books ORDER BY updated_at DESC";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
                return books;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to retrieve books list", e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public void update(Book book) {
        String sql = "UPDATE books SET title = ?, color = ?, updated_at = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, book.getTitle());
                ps.setString(2, book.getColor());
                ps.setLong(3, book.getUpdatedAt());
                ps.setString(4, book.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to update book with id " + book.getId(), e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM books WHERE id = ?";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete book with id " + id, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM books WHERE id = ? LIMIT 1";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to check book existence for id " + id, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM books";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count books", e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        return new Book(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("color"),
                rs.getLong("created_at"),
                rs.getLong("updated_at")
        );
    }
}
