package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO cho yêu cầu xe ra bãi.
 */
public record CheckoutRequest(
        @NotBlank(message = "Mã ô đỗ không được trống")
        String slotId
) {}
