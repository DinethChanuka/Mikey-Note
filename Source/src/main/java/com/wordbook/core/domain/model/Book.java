package com.wordbook.core.domain.model;

import com.wordbook.core.domain.exception.ValidationException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Domain entity representing a notebook containing multiple lessons.
 */
public class Book {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    public static final String DEFAULT_COLOR = "#2b579a";

    private final String id;
    private String title;
    private String color;
    private final long createdAt;
    private long updatedAt;

    public Book(String id, String title, String color, long createdAt, long updatedAt) {
        this.id = validateId(id);
        this.title = validateTitle(title);
        this.color = validateColor(color);
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : this.createdAt;
    }

    public static Book create(String title, String color) {
        long now = System.currentTimeMillis();
        String safeId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new Book(safeId, title, color, now, now);
    }

    public static Book createWithId(String id, String title, String color) {
        long now = System.currentTimeMillis();
        return new Book(id, title, color, now, now);
    }

    public void rename(String newTitle) {
        this.title = validateTitle(newTitle);
        this.updatedAt = System.currentTimeMillis();
    }

    public void changeColor(String newColor) {
        this.color = validateColor(newColor);
        this.updatedAt = System.currentTimeMillis();
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    private static String validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("Book ID must not be blank.");
        }
        return id.trim();
    }

    private static String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Book title must not be blank.");
        }
        String clean = title.trim();
        if (clean.length() > 255) {
            throw new ValidationException("Book title cannot exceed 255 characters.");
        }
        return clean;
    }

    private static String validateColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return DEFAULT_COLOR;
        }
        String clean = color.trim();
        // Allow composite payload: "#hex", "#hex|DESIGN_ID", or "#hex|DESIGN_ID|SUBTOPIC"
        String[] parts = clean.split("\\|", -1);
        String hexPart = parts[0].trim();
        if (!HEX_COLOR_PATTERN.matcher(hexPart).matches()) {
            return DEFAULT_COLOR;
        }
        return clean;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getColor() {
        return color;
    }

    public String getHexColor() {
        if (color == null || color.isBlank()) return DEFAULT_COLOR;
        String[] parts = color.split("\\|", -1);
        return parts[0].trim().isEmpty() ? DEFAULT_COLOR : parts[0].trim();
    }

    public String getCoverDesignId() {
        if (color == null || color.isBlank()) return "NONE";
        String[] parts = color.split("\\|", -1);
        return parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : "NONE";
    }

    public String getSubtitle() {
        if (color == null || color.isBlank()) return "";
        String[] parts = color.split("\\|", -1);
        return parts.length > 2 ? parts[2].trim() : "";
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
        if (!(o instanceof Book book)) return false;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", color='" + color + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
