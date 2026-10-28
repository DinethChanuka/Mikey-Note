package com.wordbook.core.application.dto;

public record LessonCreateRequest(
        String id,
        String title,
        String html,
        Integer sortOrder
) {}
