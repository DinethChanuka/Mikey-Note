package com.wordbook.core.application.dto;

import com.wordbook.core.domain.model.Lesson;

/**
 * Data transfer object for Lesson entity.
 */
public record LessonDto(
        String id,
        String bookId,
        String title,
        String html,
        int wordCount,
        int charCount,
        int sortOrder,
        long createdAt,
        long updatedAt
) {
    public static LessonDto fromDomain(Lesson lesson) {
        return new LessonDto(
                lesson.getId(),
                lesson.getBookId(),
                lesson.getTitle(),
                lesson.getContentHtml(),
                lesson.getWordCount(),
                lesson.getCharCount(),
                lesson.getSortOrder(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }
}
