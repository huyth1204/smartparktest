package com.smartpark.exception;

import com.smartpark.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

/**
 * Xử lý exception tập trung cho REST Controllers (ApiController, WebhookController).
 *
 * Tách biệt với MVC exception handler vì:
 * - REST trả JSON response
 * - MVC (DashboardController) redirect + flash message
 *
 * Thể hiện Single Responsibility Principle (SRP): mỗi handler chỉ lo một loại response.
 */
@RestControllerAdvice(basePackageClasses = {
        com.smartpark.controller.ApiController.class,
        com.smartpark.controller.WebhookController.class
})
public class GlobalExceptionHandler {

    /** Lỗi validate input (@Valid + Bean Validation) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Dữ liệu không hợp lệ: " + errors));
    }

    /** Tài nguyên không tìm thấy */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Ô đỗ đã có xe */
    @ExceptionHandler(SlotAlreadyOccupiedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSlotOccupied(SlotAlreadyOccupiedException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Ô đỗ đang trống */
    @ExceptionHandler(SlotNotOccupiedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSlotEmpty(SlotNotOccupiedException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Argument không hợp lệ */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Fallback — mọi exception chưa được handle */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống: " + ex.getMessage()));
    }
}
