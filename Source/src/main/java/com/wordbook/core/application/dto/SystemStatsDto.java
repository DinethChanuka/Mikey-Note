package com.wordbook.core.application.dto;

public record SystemStatsDto(
        int totalBooks,
        int totalLessons,
        long totalWords,
        long totalCharacters,
        long databaseSizeBytes,
        String databasePath,
        String javaVersion,
        long uptimeMillis
) {}
