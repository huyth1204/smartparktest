package com.smartpark.controller;

import com.smartpark.model.StaffAccount;
import com.smartpark.model.User;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountService;
import com.smartpark.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private AccountService accountService;
    @Autowired private StaffAccountRepository staffRepo;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    // ── LOGIN ──────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session, Model model) {
        // Thử đăng nhập Staff Account trước
        StaffAccount staff = staffRepo.findByUsername(username.trim())
                .filter(acc -> passwordEncoder.matches(password, acc.getPassword()))
                .filter(StaffAccount::isActive) // Chỉ cho phép tài khoản đã kích hoạt
                .orElse(null);
        
        if (staff != null) {
            session.setAttribute("user", staff);
            return "admin".equals(staff.getRole()) ? "redirect:/admin" : "redirect:/staff";
        }
        
        // Nếu không phải staff, thử User (khách hàng)
        User user = userService.login(username, password);
        if (user != null) {
            session.setAttribute("currentUser", user);
            return "redirect:/booking";
        }
        
        model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ── REGISTER ───────────────────────────────────────
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             Model model) {
        boolean ok = userService.register(username, email, password);
        if (!ok) {
            model.addAttribute("error", "Username hoặc email đã tồn tại");
            return "register";
        }
        return "redirect:/login?registered=true";
    }

    // ── FORGOT PASSWORD (User) ────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgot(@RequestParam String email,
                           HttpServletRequest request, Model model) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        boolean sent = userService.sendResetLink(email, baseUrl);
        // Luôn hiện thông báo thành công (tránh lộ email có tồn tại không)
        model.addAttribute("success",
            "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi.");
        return "forgot-password";
    }

    // ── RESET PASSWORD (User) ─────────────────────────────────
    @GetMapping("/reset-password")
    public String resetPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String doReset(@RequestParam String token,
                          @RequestParam String newPassword,
                          Model model) {
        String result = userService.resetPassword(token, newPassword);
        if ("OK".equals(result)) return "redirect:/login?reset=true";
        model.addAttribute("error", result);
        model.addAttribute("token", token);
        return "reset-password";
    }

    // ── FORGOT PASSWORD (Staff) ────────────────────────────────
    @GetMapping("/staff/forgot-password")
    public String staffForgotPage() { return "staff-forgot-password"; }

    @PostMapping("/staff/forgot-password")
    public String doStaffForgot(@RequestParam String email,
                                HttpServletRequest request, Model model) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        boolean sent = accountService.sendStaffResetLink(email, baseUrl);
        // Luôn hiện thông báo thành công (tránh lộ email có tồn tại không)
        model.addAttribute("success",
            "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi.");
        return "staff-forgot-password";
    }

    // ── RESET PASSWORD (Staff) ─────────────────────────────────
    @GetMapping("/staff/reset-password")
    public String staffResetPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "staff-reset-password";
    }

    @PostMapping("/staff/reset-password")
    public String doStaffReset(@RequestParam String token,
                               @RequestParam String newPassword,
                               Model model) {
        String result = accountService.resetStaffPassword(token, newPassword);
        if ("OK".equals(result)) return "redirect:/login?reset=true";
        model.addAttribute("error", result);
        model.addAttribute("token", token);
        return "staff-reset-password";
    }
}