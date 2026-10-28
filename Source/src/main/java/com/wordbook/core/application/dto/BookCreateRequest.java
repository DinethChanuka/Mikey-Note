package com.wordbook.core.application.dto;

public record BookCreateRequest(
        String id,
        String title,
        String color
) {}
