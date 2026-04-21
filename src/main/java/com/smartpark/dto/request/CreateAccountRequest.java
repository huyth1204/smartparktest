package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO cho yêu cầu tạo tài khoản nhân viên.
 */
public record CreateAccountRequest(
        @NotBlank(message = "Họ tên không được trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Tên đăng nhập không được trống")
        @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3-50 ký tự")
        String username,

        String email,

        @NotBlank(message = "Mật khẩu không được trống")
        @Size(min = 4, message = "Mật khẩu tối thiểu 4 ký tự")
        String password,

        @NotBlank(message = "Vai trò không được trống")
        @Pattern(regexp = "^(admin|staff)$", message = "Vai trò chỉ là 'admin' hoặc 'staff'")
        String role
) {}
