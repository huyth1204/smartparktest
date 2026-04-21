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

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session, Model model) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin");
            return "login";
        }
        return staffRepo.findByUsername(username.trim())
                .filter(acc -> passwordEncoder.matches(password, acc.getPassword()))
                .map(acc -> {
                    session.setAttribute("user", acc);
                    return "admin".equals(acc.getRole()) ? "redirect:/admin" : "redirect:/staff";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng");
                    return "login";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ── VERIFY ACCOUNT ────────────────────────────────────────────────────────

    @GetMapping("/verify-account")
    public String verifyAccountPage(@RequestParam String token, Model model) {
        String result = verificationService.verifyAccount(token);
        if ("OK".equals(result)) {
            model.addAttribute("success", "Xác nhận tài khoản thành công! Bạn có thể đăng nhập ngay.");
        } else {
            model.addAttribute("error", result);
        }
        return "login";
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
                             @RequestParam String password,
                             @RequestParam(required = false) String email,
                             @RequestParam String role,
                             HttpSession session,
                             HttpServletRequest request,
                             RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
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
        
        String tempPassword = password; // Lưu mật khẩu tạm để gửi email
        StaffAccount acc = new StaffAccount(code, fullName.trim(), username.trim(), passwordEncoder.encode(password), role);
        
        if (StringUtils.hasText(email)) {
            acc.setEmail(email.trim().toLowerCase());
            acc.setVerified(false); // Chưa xác nhận
            acc.setActive(false);   // Chưa kích hoạt
        }
        
        staffRepo.save(acc);
        
        // Gửi email xác nhận nếu có email
        if (StringUtils.hasText(email)) {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                           + (request.getServerPort() != 80 && request.getServerPort() != 443 
                              ? ":" + request.getServerPort() : "");
            verificationService.sendVerificationEmail(acc, baseUrl, tempPassword);
            ra.addFlashAttribute("success", "Đã tạo tài khoản " + username.trim() + ". Email xác nhận đã được gửi đến " + email);
        } else {
            acc.setVerified(true);
            acc.setActive(true);
            staffRepo.save(acc);
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
