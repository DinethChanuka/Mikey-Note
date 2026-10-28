package com.wordbook.core.application.dto;

import com.wordbook.core.domain.model.Book;

/**
 * Data transfer object for Book entity.
 */
public record BookDto(
        String id,
        String title,
        String color,
        int lessonCount,
        long createdAt,
        long updatedAt
) {
    public static BookDto fromDomain(Book book, int lessonCount) {
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getColor(),
                lessonCount,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
