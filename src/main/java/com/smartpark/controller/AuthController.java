package com.smartpark.controller;

import com.smartpark.model.User;
import com.smartpark.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired private UserService userService;

    // ── LOGIN ──────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session, Model model) {
        User user = userService.login(username, password);
        if (user == null) {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
            return "login";
        }
        session.setAttribute("currentUser", user);
        return "redirect:/booking";
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

    // ── FORGOT PASSWORD ────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgot(@RequestParam String email,
                           HttpServletRequest request, Model model) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                         + ":" + request.getServerPort();
        boolean sent = userService.sendResetLink(email, baseUrl);
        // Luôn hiện thông báo thành công (tránh lộ email có tồn tại không)
        model.addAttribute("success",
            "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi.");
        return "forgot-password";
    }

    // ── RESET PASSWORD ─────────────────────────────────
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
}