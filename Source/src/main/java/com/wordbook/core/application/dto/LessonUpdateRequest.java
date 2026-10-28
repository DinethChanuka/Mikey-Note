package com.wordbook.core.application.dto;

public record LessonUpdateRequest(
        String title,
        String html,
        Integer wordCount,
        Integer charCount,
        Integer sortOrder
) {}
