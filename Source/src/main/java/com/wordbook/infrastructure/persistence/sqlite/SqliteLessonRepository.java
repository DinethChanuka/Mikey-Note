package com.wordbook.infrastructure.persistence.sqlite;

import com.wordbook.core.domain.exception.PersistenceException;
import com.wordbook.core.domain.model.Lesson;
import com.wordbook.core.domain.repository.LessonRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite JDBC implementation of LessonRepository using PreparedStatements.
 */
public class SqliteLessonRepository implements LessonRepository {
    private final SqliteConnectionManager connectionManager;

    public SqliteLessonRepository(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void save(Lesson lesson) {
        String sql = """
            INSERT INTO lessons (id, book_id, title, content_html, word_count, char_count, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, lesson.getId());
                ps.setString(2, lesson.getBookId());
                ps.setString(3, lesson.getTitle());
                ps.setString(4, lesson.getContentHtml());
                ps.setInt(5, lesson.getWordCount());
                ps.setInt(6, lesson.getCharCount());
                ps.setInt(7, lesson.getSortOrder());
                ps.setLong(8, lesson.getCreatedAt());
                ps.setLong(9, lesson.getUpdatedAt());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save lesson with id " + lesson.getId(), e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Lesson> findById(String id) {
        String sql = """
            SELECT id, book_id, title, content_html, word_count, char_count, sort_order, created_at, updated_at
            FROM lessons WHERE id = ?
        """;
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToLesson(rs));
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find lesson by id " + id, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public List<Lesson> findByBookId(String bookId) {
        String sql = """
            SELECT id, book_id, title, content_html, word_count, char_count, sort_order, created_at, updated_at
            FROM lessons WHERE book_id = ?
            ORDER BY sort_order ASC, created_at ASC
        """;
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Lesson> lessons = new ArrayList<>();
                    while (rs.next()) {
                        lessons.add(mapResultSetToLesson(rs));
                    }
                    return lessons;
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find lessons for bookId " + bookId, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public void update(Lesson lesson) {
        String sql = """
            UPDATE lessons
            SET title = ?, content_html = ?, word_count = ?, char_count = ?, sort_order = ?, updated_at = ?
            WHERE id = ?
        """;
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, lesson.getTitle());
                ps.setString(2, lesson.getContentHtml());
                ps.setInt(3, lesson.getWordCount());
                ps.setInt(4, lesson.getCharCount());
                ps.setInt(5, lesson.getSortOrder());
                ps.setLong(6, lesson.getUpdatedAt());
                ps.setString(7, lesson.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to update lesson with id " + lesson.getId(), e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM lessons WHERE id = ?";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete lesson with id " + id, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public int deleteByBookId(String bookId) {
        String sql = "DELETE FROM lessons WHERE book_id = ?";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, bookId);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete lessons for bookId " + bookId, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM lessons WHERE id = ? LIMIT 1";
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
            throw new PersistenceException("Failed to check lesson existence for id " + id, e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM lessons";
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
            throw new PersistenceException("Failed to count lessons", e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public long sumTotalWords() {
        String sql = "SELECT COALESCE(SUM(word_count), 0) FROM lessons";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to sum total words", e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    @Override
    public long sumTotalCharacters() {
        String sql = "SELECT COALESCE(SUM(char_count), 0) FROM lessons";
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to sum total characters", e);
        } finally {
            connectionManager.releaseConnection(conn);
        }
    }

    private Lesson mapResultSetToLesson(ResultSet rs) throws SQLException {
        return new Lesson(
                rs.getString("id"),
                rs.getString("book_id"),
                rs.getString("title"),
                rs.getString("content_html"),
                rs.getInt("word_count"),
                rs.getInt("char_count"),
                rs.getInt("sort_order"),
                rs.getLong("created_at"),
                rs.getLong("updated_at")
        );
    }
}
