package com.wordbook.core.application.dto;

import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.model.Lesson;

import java.util.List;

/**
 * Composite DTO representing a Book together with its ordered Lessons.
 */
public record BookWithLessonsDto(
        String id,
        String title,
        String color,
        long createdAt,
        long updatedAt,
        List<LessonDto> lessons
) {
    public static BookWithLessonsDto fromDomain(Book book, List<Lesson> lessons) {
        List<LessonDto> lessonDtos = lessons.stream()
                .map(LessonDto::fromDomain)
                .toList();
        return new BookWithLessonsDto(
                book.getId(),
                book.getTitle(),
                book.getColor(),
                book.getCreatedAt(),
                book.getUpdatedAt(),
                lessonDtos
        );
    }
}
