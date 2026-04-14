package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO cho yêu cầu xe vào bãi.
 */
public record CheckinRequest(
        @NotBlank(message = "Mã ô đỗ không được trống")
        String slotId,

        @NotBlank(message = "Biển số xe không được trống")
        String licensePlate
) {}
