package com.smartpark.controller;

import com.smartpark.dto.request.CheckinRequest;
import com.smartpark.dto.request.CheckoutRequest;
import com.smartpark.dto.request.CreateAccountRequest;
import com.smartpark.dto.request.LoginRequest;
import com.smartpark.dto.request.UpdateAccountRequest;
import com.smartpark.dto.response.ApiResponse;
import com.smartpark.dto.response.StaffAccountResponse;
import com.smartpark.model.Booking;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.BookingService;
import com.smartpark.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final ParkingService parkingService;
    private final StaffAccountRepository staffRepo;
    private final BookingRepository bookingRepo;
    private final BookingService bookingService;
    private final BCryptPasswordEncoder passwordEncoder;

    public ApiController(ParkingService parkingService,
                         StaffAccountRepository staffRepo,
                         BookingRepository bookingRepo,
                         BookingService bookingService,
                         BCryptPasswordEncoder passwordEncoder) {
        this.parkingService  = parkingService;
        this.staffRepo       = staffRepo;
        this.bookingRepo     = bookingRepo;
        this.bookingService  = bookingService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── AUTH ──────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<StaffAccountResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return staffRepo.findByUsername(req.username())
                .filter(acc -> passwordEncoder.matches(req.password(), acc.getPassword()))
                .map(acc -> ResponseEntity.ok(
                        ApiResponse.success("Đăng nhập thành công", StaffAccountResponse.from(acc))))
                .orElse(ResponseEntity.ok(ApiResponse.error("Sai tên đăng nhập hoặc mật khẩu")));
    }

    // ── SLOTS ─────────────────────────────────────────────────────────────────

    @GetMapping("/slots")
    public ApiResponse<List<ParkingSlot>> getAllSlots() {
        return ApiResponse.success(parkingService.getAllSlots());
    }

    @GetMapping("/slots/zone/{zone}")
    public ApiResponse<List<ParkingSlot>> getByZone(@PathVariable String zone) {
        return ApiResponse.success(parkingService.getSlotsByZone(zone));
    }

    @GetMapping("/slots/stats")
    public ApiResponse<?> getStats() {
        return ApiResponse.success(parkingService.getStats());
    }

    @GetMapping("/slots/search")
    public ApiResponse<List<ParkingSlot>> search(@RequestParam String plate) {
        return ApiResponse.success(parkingService.searchByPlate(plate));
    }

    /**
     * Checkin — dùng @Valid + DTO thay vì Map thô.
     * Bean Validation tự validate trước khi vào method.
     */
    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<ParkingSlot>> checkin(
            @Valid @RequestBody CheckinRequest req) {
        ParkingSlot slot = parkingService.checkin(req.slotId(), req.licensePlate());
        return ResponseEntity.ok(ApiResponse.success("Xe đã vào ô " + slot.getId(), slot));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<ParkingSlot>> checkout(
            @Valid @RequestBody CheckoutRequest req) {
        ParkingSlot slot = parkingService.checkout(req.slotId());
        return ResponseEntity.ok(ApiResponse.success("Xe đã ra khỏi ô " + slot.getId(), slot));
    }

    // ── BOOKINGS ──────────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ApiResponse<List<Booking>> getBookings() {
        return ApiResponse.success(bookingService.getAll());
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBooking(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)))
                .orElse(ResponseEntity.ok(ApiResponse.error("Không tìm thấy vé #" + id)));
    }

    // ── ACCOUNTS ──────────────────────────────────────────────────────────────

    @GetMapping("/accounts")
    public ApiResponse<List<StaffAccountResponse>> getAccounts() {
        List<StaffAccountResponse> list = staffRepo.findAllByOrderByStaffCodeAsc()
                .stream()
                .map(StaffAccountResponse::from) // Method reference — Java functional style
                .toList();
        return ApiResponse.success(list);
    }

    /**
     * Tạo tài khoản — dùng @Valid DTO, check duplicate username.
     */
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<StaffAccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest req) {
        if (staffRepo.existsByUsernameIgnoreCase(req.username())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tên đăng nhập đã tồn tại"));
        }
        String prefix = "admin".equals(req.role()) ? "AD" : "NV";
        long count = staffRepo.findAllByOrderByStaffCodeAsc().stream()
                .filter(a -> a.getStaffCode().startsWith(prefix)).count();
        String code = prefix + String.format("%03d", count + 1);

        StaffAccount saved = staffRepo.save(
                new StaffAccount(code, req.fullName(), req.username(), req.email(), passwordEncoder.encode(req.password()), req.role()));
        return ResponseEntity.ok(ApiResponse.success("Đã tạo tài khoản", StaffAccountResponse.from(saved)));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<StaffAccountResponse>> updateAccount(
            @PathVariable Long id, @Valid @RequestBody UpdateAccountRequest req) {
        return staffRepo.findById(id).map(acc -> {
            acc.setFullName(req.fullName());
            acc.setEmail(req.email());
            acc.setRole(req.role());
            acc.setActive(req.active());
            if (req.password() != null && !req.password().isBlank())
                acc.setPassword(passwordEncoder.encode(req.password()));
            return ResponseEntity.ok(
                    ApiResponse.success(StaffAccountResponse.from(staffRepo.save(acc))));
        }).orElse(ResponseEntity.ok(ApiResponse.error("Không tìm thấy tài khoản #" + id)));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        if (!staffRepo.existsById(id))
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy tài khoản #" + id));
        staffRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá tài khoản", null));
    }

    // ── RESET ─────────────────────────────────────────────────────────────────

    @PostMapping("/reset/slots")
    public ApiResponse<Void> resetSlots() {
        parkingService.resetAllSlots();
        return ApiResponse.success("Đã reset tất cả ô đỗ", null);
    }

    @PostMapping("/reset/all")
    public ApiResponse<Void> resetAll() {
        bookingRepo.deleteAll();
        parkingService.resetAllSlots();
        return ApiResponse.success("Đã reset toàn bộ hệ thống", null);
    }
}
