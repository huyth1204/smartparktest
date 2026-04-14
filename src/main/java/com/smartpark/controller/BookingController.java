package com.smartpark.controller;

import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.model.Booking;
import com.smartpark.service.BookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import com.smartpark.service.ParkingService;

/**
 * Controller xử lý đặt vé & hiển thị chi tiết vé.
 */
@Controller
public class BookingController {

    private final BookingService bookingService;
    private final ParkingService parkingService;

    @Value("${app.bank.account}") private String bankAccount;
    @Value("${app.bank.owner}")   private String bankOwner;
    @Value("${app.bank.name}")    private String bankName;

    public BookingController(BookingService bookingService, ParkingService parkingService) {
        this.bookingService = bookingService;
        this.parkingService = parkingService;
    }

    // Redirect root về login
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    // Trang đặt vé (booking form)
    @GetMapping("/booking")
    public String booking() {
        return "index";
    }

    // Xử lý form đặt vé
    @PostMapping("/dat-ve")
    public String datVe(
            @RequestParam String customerName,
            @RequestParam String licensePlate,
            @RequestParam String vehicleType,
            @RequestParam(required = false) String slotPreference,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
            Model model) {

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            model.addAttribute("error", "Thời gian ra phải sau thời gian vào");
            return "index";
        }

        Booking b = bookingService.createBooking(customerName, licensePlate, vehicleType, checkIn, checkOut);

        // Tự động giữ ô đỗ nếu khách chọn
        if (slotPreference != null && !slotPreference.isBlank()) {
            parkingService.reserveSlot(slotPreference, licensePlate);
        }

        return "redirect:/ve/" + b.getId();
    }

    // Trang chi tiết vé + QR thanh toán
    @GetMapping("/ve/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        Booking b = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vé", "id", id));

        model.addAttribute("booking", b);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner",   bankOwner);
        model.addAttribute("bankName",    bankName);

        // URL QR VietQR – quét bằng app ngân hàng bất kỳ
        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            bankName, bankAccount,
            b.getAmountDue(),
            b.getPaymentCode(),
            bankOwner.replace(" ", "%20")
        );
        model.addAttribute("qrUrl", qrUrl);

        return "chi-tiet";
    }

    // Kiểm tra trạng thái (dùng cho auto-refresh JS)
    @GetMapping("/api/status/{id}")
    @ResponseBody
    public String checkStatus(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(Booking::getStatus)
                .orElse("NOT_FOUND");
    }
}
