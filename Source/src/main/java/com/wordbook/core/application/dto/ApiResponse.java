package com.wordbook.core.application.dto;

/**
 * Standardized JSON API response container.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        long timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, System.currentTimeMillis());
    }
}
