package com.smartpark.dto.response;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper — dùng Generics (tính năng Java nâng cao).
 *
 * @param <T> kiểu dữ liệu của data trả về
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp
) {
    /** Tạo response thành công */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, LocalDateTime.now());
    }

    /** Tạo response thành công có message */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    /** Tạo response lỗi */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
