package com.smartpark.controller;

import com.smartpark.model.StaffAccount;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountVerificationService;
import com.smartpark.service.BookingService;
import com.smartpark.service.ParkingService;
import com.smartpark.service.ParkingSlotService.SlotStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Controller
public class DashboardController {

    private final ParkingService slotService;
    private final StaffAccountRepository staffRepo;
    private final BookingService bookingService;
    private final BookingRepository bookingRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AccountVerificationService verificationService;

    public DashboardController(ParkingService slotService,
                               StaffAccountRepository staffRepo,
                               BookingService bookingService,
                               BookingRepository bookingRepo,
                               BCryptPasswordEncoder passwordEncoder,
                               AccountVerificationService verificationService) {
        this.slotService          = slotService;
        this.staffRepo            = staffRepo;
        this.bookingService       = bookingService;
        this.bookingRepo          = bookingRepo;
        this.passwordEncoder      = passwordEncoder;
        this.verificationService  = verificationService;
    }

    // ── VERIFY ACCOUNT ────────────────────────────────────────────────────────

    @GetMapping("/verify-account")
    public String verifyAccountPage(@RequestParam String token, Model model) {
        String result = verificationService.verifyAccount(token);
        if ("OK".equals(result)) {
            model.addAttribute("token", token);
            return "set-password";
        } else {
            model.addAttribute("error", result);
            return "login";
        }
    }

    // ── SET PASSWORD ──────────────────────────────────────────────────────────

    @GetMapping("/set-password")
    public String setPasswordPage(@RequestParam String token, Model model) {
        // Validate token first
        String validation = verificationService.validateToken(token);
        if (!"OK".equals(validation)) {
            model.addAttribute("error", validation);
            return "login";
        }
        model.addAttribute("token", token);
        return "set-password";
    }

    @PostMapping("/set-password")
    public String doSetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes ra) {
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/set-password?token=" + token;
        }
        
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
            return "redirect:/set-password?token=" + token;
        }
        
        String result = verificationService.setPassword(token, password);
        if ("OK".equals(result)) {
            ra.addFlashAttribute("success", "Đặt mật khẩu thành công! Bạn có thể đăng nhập ngay.");
        } else {
            ra.addFlashAttribute("error", result);
        }
        return "redirect:/login";
    }

    // ── PUBLIC MAP ────────────────────────────────────────────────────────────

    @GetMapping("/map")
    public String publicMap(Model model) {
        addStatsToModel(model);
        return "public-map";
    }

    // ── STAFF DASHBOARD ───────────────────────────────────────────────────────

    @GetMapping("/staff")
    public String staffDashboard(HttpSession session, Model model) {
        StaffAccount user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        addStatsToModel(model);
        return "staff-dashboard";
    }

    @PostMapping("/staff/checkin")
    public String checkin(@RequestParam String slotId,
                          @RequestParam String licensePlate,
                          HttpSession session,
                          RedirectAttributes ra) {
        if (requireLogin(session) == null) return "redirect:/login";
        try {
            slotService.checkin(slotId, licensePlate);
            ra.addFlashAttribute("success", "Xe " + licensePlate.toUpperCase() + " đã vào ô " + slotId.toUpperCase());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/staff/checkout")
    public String checkout(@RequestParam String slotId,
                           HttpSession session,
                           RedirectAttributes ra) {
        if (requireLogin(session) == null) return "redirect:/login";
        try {
            var slot = slotService.checkout(slotId);
            ra.addFlashAttribute("success", "Xe đã ra khỏi ô " + slot.getId());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    // ── ADMIN DASHBOARD ───────────────────────────────────────────────────────

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        StaffAccount user = requireAdmin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        addStatsToModel(model);
        List<StaffAccount> accounts = staffRepo.findAllByOrderByStaffCodeAsc();
        model.addAttribute("accounts", accounts);
        model.addAttribute("bookings", bookingService.getAll());
        return "admin-dashboard";
    }

    @PostMapping("/admin/accounts/add")
    public String addAccount(@RequestParam String fullName,
                             @RequestParam String username,
                             @RequestParam(required = false) String email,
                             @RequestParam String role,
                             HttpSession session,
                             HttpServletRequest request,
                             RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(username)) {
            ra.addFlashAttribute("error", "Vui lòng nhập đầy đủ thông tin tài khoản");
            return "redirect:/admin";
        }
        if (staffRepo.existsByUsernameIgnoreCase(username.trim())) {
            ra.addFlashAttribute("error", "Tên đăng nhập đã tồn tại");
            return "redirect:/admin";
        }
        
        String prefix = "admin".equals(role) ? "AD" : "NV";
        long count = staffRepo.findAllByOrderByStaffCodeAsc().stream()
                .filter(a -> a.getStaffCode().startsWith(prefix))
                .count();
        String code = prefix + String.format("%03d", count + 1);
        
        String emailToUse = StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
        // For accounts with email, password will be set during verification
        // For accounts without email, set a temporary password that must be changed
        String tempPassword = StringUtils.hasText(email) ? "" : "changeme123";
        StaffAccount acc = new StaffAccount(code, fullName.trim(), username.trim(), emailToUse, 
                                           StringUtils.hasText(email) ? "" : passwordEncoder.encode(tempPassword), role);
        
        if (StringUtils.hasText(email)) {
            acc.setVerified(false); // Chưa xác nhận
            acc.setActive(false);   // Chưa kích hoạt
        } else {
            acc.setVerified(true);
            acc.setActive(true);
        }
        
        staffRepo.save(acc);
        
        // Gửi email xác nhận nếu có email
        if (StringUtils.hasText(email)) {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                           + (request.getServerPort() != 80 && request.getServerPort() != 443 
                              ? ":" + request.getServerPort() : "");
            verificationService.sendVerificationEmail(acc, baseUrl);
            ra.addFlashAttribute("success", "Đã tạo tài khoản " + username.trim() + ". Email xác nhận đã được gửi đến " + email);
            ra.addFlashAttribute("newAccountId", acc.getId());
        } else {
            ra.addFlashAttribute("success", "Đã thêm tài khoản " + username.trim());
        }
        
        return "redirect:/admin";
    }

    @PostMapping("/admin/accounts/update")
    public String updateAccount(@RequestParam Long id,
                                @RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam(required = false) String password,
                                @RequestParam(required = false) String email,
                                @RequestParam String role,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        staffRepo.findById(id).ifPresentOrElse(acc -> {
            acc.setFullName(fullName.trim());
            acc.setUsername(username.trim());
            if (StringUtils.hasText(password)) acc.setPassword(passwordEncoder.encode(password));
            if (StringUtils.hasText(email)) acc.setEmail(email.trim().toLowerCase());
            acc.setRole(role);
            staffRepo.save(acc);
            ra.addFlashAttribute("success", "Đã cập nhật tài khoản " + username.trim());
        }, () -> ra.addFlashAttribute("error", "Không tìm thấy tài khoản"));
        return "redirect:/admin";
    }

    @PostMapping("/admin/accounts/delete")    public String deleteAccount(@RequestParam Long id,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        staffRepo.findById(id).ifPresentOrElse(
                acc -> { staffRepo.deleteById(id); ra.addFlashAttribute("success", "Đã xoá tài khoản " + acc.getUsername()); },
                ()  -> ra.addFlashAttribute("error", "Không tìm thấy tài khoản")
        );
        return "redirect:/admin";
    }

    @GetMapping("/admin/accounts/verify-status/{id}")
    @ResponseBody
    public java.util.Map<String, String> getVerifyStatus(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) {
            return java.util.Map.of("status", "unauthorized");
        }
        
        return staffRepo.findById(id)
            .map(acc -> {
                if (acc.isVerified() && acc.isActive()) {
                    return java.util.Map.of("status", "verified");
                } else if (verificationService.isTokenExpired(acc)) {
                    return java.util.Map.of("status", "expired");
                } else {
                    return java.util.Map.of("status", "pending");
                }
            })
            .orElse(java.util.Map.of("status", "not_found"));
    }

    @PostMapping("/admin/reset/slots")
    public String resetSlots(HttpSession session, RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        slotService.resetAllSlots();
        ra.addFlashAttribute("success", "Đã reset tất cả ô đỗ về trạng thái trống");
        return "redirect:/admin";
    }

    @PostMapping("/admin/reset/all")
    public String resetAll(HttpSession session, RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        bookingRepo.deleteAll();
        slotService.resetAllSlots();
        ra.addFlashAttribute("success", "Đã reset toàn bộ hệ thống");
        return "redirect:/admin";
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void addStatsToModel(Model model) {
        SlotStats s = slotService.getStats();
        model.addAttribute("total",      s.total());
        model.addAttribute("filled",     s.filled());
        model.addAttribute("empty",      s.empty());
        model.addAttribute("motoTotal",  s.motoTotal());
        model.addAttribute("motoFilled", s.motoFilled());
        model.addAttribute("motoEmpty",  s.motoEmpty());
        model.addAttribute("carTotal",   s.carTotal());
        model.addAttribute("carFilled",  s.carFilled());
        model.addAttribute("carEmpty",   s.carEmpty());
        model.addAttribute("pct",        s.pct());
        model.addAttribute("motoSlots",  slotService.getSlotsByZone("motorbike"));
        model.addAttribute("carSlots",   slotService.getSlotsByZone("car"));
        model.addAttribute("motoRows",   List.of("A","B","C","D","E"));
        model.addAttribute("carRows",    List.of("A","B","C"));
    }

    private StaffAccount requireLogin(HttpSession session) {
        return (StaffAccount) session.getAttribute("user");
    }

    private StaffAccount requireAdmin(HttpSession session) {
        StaffAccount u = requireLogin(session);
        return (u != null && "admin".equals(u.getRole())) ? u : null;
    }
}
