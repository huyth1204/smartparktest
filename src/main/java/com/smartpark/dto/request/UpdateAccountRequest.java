package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO cho yêu cầu cập nhật tài khoản.
 */
public record UpdateAccountRequest(
        @NotBlank(message = "Họ tên không được trống")
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Pattern(regexp = "^(admin|staff)$", message = "Vai trò chỉ là 'admin' hoặc 'staff'")
        String role,

        boolean active,

        /** Có thể null/blank nếu không đổi mật khẩu */
        String password
) {}
