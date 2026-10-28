package com.wordbook.core.domain.model;

import com.wordbook.core.domain.exception.ValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an individual lesson or note document inside a Book.
 */
public class Lesson {
    private final String id;
    private final String bookId;
    private String title;
    private String contentHtml;
    private int wordCount;
    private int charCount;
    private int sortOrder;
    private final long createdAt;
    private long updatedAt;

    public Lesson(String id, String bookId, String title, String contentHtml,
                  int wordCount, int charCount, int sortOrder, long createdAt, long updatedAt) {
        this.id = validateId(id, "Lesson ID");
        this.bookId = validateId(bookId, "Book ID reference");
        this.title = validateTitle(title);
        this.contentHtml = contentHtml != null ? contentHtml : "<p><br></p>";
        this.wordCount = Math.max(0, wordCount);
        this.charCount = Math.max(0, charCount);
        this.sortOrder = Math.max(0, sortOrder);
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : this.createdAt;
    }

    public static Lesson create(String bookId, String title, String initialHtml, int sortOrder) {
        long now = System.currentTimeMillis();
        String safeId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String html = (initialHtml != null && !initialHtml.trim().isEmpty())
                ? initialHtml
                : "<h1>" + escapeHtml(title) + "</h1><p><br></p>";
        Lesson lesson = new Lesson(safeId, bookId, title, html, 0, 0, sortOrder, now, now);
        lesson.recalculateMetrics();
        return lesson;
    }

    public static Lesson createWithId(String id, String bookId, String title, String html, int sortOrder) {
        long now = System.currentTimeMillis();
        Lesson lesson = new Lesson(id, bookId, title, html, 0, 0, sortOrder, now, now);
        lesson.recalculateMetrics();
        return lesson;
    }

    public void updateTitle(String newTitle) {
        this.title = validateTitle(newTitle);
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateContent(String newHtml) {
        this.contentHtml = newHtml != null ? newHtml : "<p><br></p>";
        recalculateMetrics();
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateContentWithMetrics(String newHtml, int words, int chars) {
        this.contentHtml = newHtml != null ? newHtml : "<p><br></p>";
        this.wordCount = Math.max(0, words);
        this.charCount = Math.max(0, chars);
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateSortOrder(int newOrder) {
        this.sortOrder = Math.max(0, newOrder);
        this.updatedAt = System.currentTimeMillis();
    }

    public void recalculateMetrics() {
        if (contentHtml == null || contentHtml.isEmpty()) {
            this.wordCount = 0;
            this.charCount = 0;
            return;
        }
        String plainText = contentHtml.replaceAll("<[^>]*>", " ").replaceAll("&[^;]+;", " ").trim();
        this.charCount = plainText.length();
        if (plainText.isEmpty()) {
            this.wordCount = 0;
        } else {
            String[] words = plainText.split("\\s+");
            this.wordCount = words.length;
        }
    }

    private static String validateId(String id, String fieldName) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException(fieldName + " must not be blank.");
        }
        return id.trim();
    }

    private static String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Lesson title must not be blank.");
        }
        String clean = title.trim();
        if (clean.length() > 255) {
            throw new ValidationException("Lesson title cannot exceed 255 characters.");
        }
        return clean;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public String getId() {
        return id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getCharCount() {
        return charCount;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lesson lesson)) return false;
        return Objects.equals(id, lesson.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Lesson{" +
                "id='" + id + '\'' +
                ", bookId='" + bookId + '\'' +
                ", title='" + title + '\'' +
                ", wordCount=" + wordCount +
                ", charCount=" + charCount +
                ", sortOrder=" + sortOrder +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
