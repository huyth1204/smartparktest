package com.smartpark.controller;

import com.smartpark.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final BookingService service;
    public WebhookController(BookingService service) { this.service = service; }

    /**
     * SePay POST về đây khi có tiền vào tài khoản.
     * Điền URL này vào SePay: https://smartpark-2.onrender.com/webhook/sepay
     */
    @PostMapping("/sepay")
    public ResponseEntity<Map<String, Object>> handleSePay(
            @RequestBody Map<String, Object> payload) {

        System.out.println("[WEBHOOK] " + payload);

        String content      = (String) payload.get("content");
        String transferType = (String) payload.get("transferType");
        Object amountRaw    = payload.get("transferAmount");
        long   amount       = amountRaw instanceof Number n ? n.longValue() : 0L;
        String bankRef      = String.valueOf(payload.getOrDefault("referenceCode", ""));

        if (!"in".equals(transferType))
            return ResponseEntity.ok(Map.of("success", true, "message", "ignored"));

        boolean ok = service.processPayment(content, amount, bankRef);
        return ResponseEntity.ok(Map.of("success", true, "confirmed", ok));
    }

    /**
     * Test thủ công – dùng khi demo không có chuyển khoản thật
     * Gọi bằng trình duyệt:
     * GET https://smartpark-2.onrender.com/webhook/test-pay?code=SPXXXXXX&amount=5000
     */
    @GetMapping("/test-pay")
    public ResponseEntity<Map<String, Object>> testPay(
            @RequestParam String code,
            @RequestParam long amount) {

        System.out.println("[TEST-PAY] code=" + code + " amount=" + amount);
        boolean ok = service.processPayment(code, amount, "TEST-REF");
        return ResponseEntity.ok(Map.of("success", true, "confirmed", ok));
    }
}