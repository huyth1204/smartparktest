package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO cho yêu cầu đăng nhập.
 * Sử dụng Java Record (Java 17) — immutable, tự tạo getter/equals/hashCode.
 */
public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được trống")
        @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3-50 ký tự")
        String username,

        @NotBlank(message = "Mật khẩu không được trống")
        String password
) {}
