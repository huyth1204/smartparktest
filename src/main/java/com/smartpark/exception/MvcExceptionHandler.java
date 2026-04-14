package com.smartpark.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Xử lý exception cho SSR Controllers (DashboardController, BookingController).
 *
 * Thay vì trả trang lỗi trắng, redirect về trang trước với flash message.
 * Thể hiện Separation of Concerns: tách exception handling ra khỏi controller logic.
 */
@ControllerAdvice(basePackageClasses = {
        com.smartpark.controller.DashboardController.class,
        com.smartpark.controller.BookingController.class
})
public class MvcExceptionHandler {

    /** Ô đỗ đã có xe → redirect về trang trước với thông báo lỗi */
    @ExceptionHandler(SlotAlreadyOccupiedException.class)
    public String handleSlotOccupied(SlotAlreadyOccupiedException ex,
                                     RedirectAttributes ra,
                                     HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + getReferer(request, "/staff");
    }

    /** Ô đỗ đang trống */
    @ExceptionHandler(SlotNotOccupiedException.class)
    public String handleSlotEmpty(SlotNotOccupiedException ex,
                                  RedirectAttributes ra,
                                  HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + getReferer(request, "/staff");
    }

    /** Tài nguyên không tìm thấy */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex,
                                 RedirectAttributes ra,
                                 HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + getReferer(request, "/");
    }

    /** Argument không hợp lệ (validation thủ công) */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex,
                                        RedirectAttributes ra,
                                        HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + getReferer(request, "/");
    }

    /** Fallback — lỗi không xác định */
    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex,
                                RedirectAttributes ra,
                                HttpServletRequest request) {
        ra.addFlashAttribute("error", "Lỗi hệ thống: " + ex.getMessage());
        return "redirect:" + getReferer(request, "/");
    }

    // Lấy URL trang trước từ Referer header, fallback về defaultUrl
    private String getReferer(HttpServletRequest request, String defaultUrl) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) return defaultUrl;
        // Chỉ lấy path, bỏ domain để tránh open redirect
        try {
            java.net.URI uri = new java.net.URI(referer);
            String path = uri.getPath();
            return (path != null && !path.isBlank()) ? path : defaultUrl;
        } catch (Exception e) {
            return defaultUrl;
        }
    }
}
