package com.wordbook.core.domain.model;

/**
 * Value object summarizing system-wide and notebook statistics.
 */
public record BookStatistics(
        int totalBooks,
        int totalLessons,
        long totalWords,
        long totalCharacters,
        long databaseSizeBytes,
        String databasePath
) {}
