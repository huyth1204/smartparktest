package com.smartpark.service;

import com.smartpark.model.PasswordResetToken;
import com.smartpark.model.User;
import com.smartpark.repository.PasswordResetTokenRepository;
import com.smartpark.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordResetTokenRepository tokenRepo;
    @Autowired private EmailService emailService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Đăng ký
    public boolean register(String username, String email, String rawPassword) {
        if (userRepo.existsByEmail(email) || userRepo.existsByUsername(username)) return false;
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPassword));
        userRepo.save(u);
        return true;
    }

    // Đăng nhập
    public User login(String username, String rawPassword) {
        return userRepo.findByUsername(username)
                .filter(u -> encoder.matches(rawPassword, u.getPassword()))
                .orElse(null);
    }

    // Gửi email reset mật khẩu
    @Transactional
    public boolean sendResetLink(String email, String baseUrl) {
        return userRepo.findByEmail(email).map(user -> {
            // Xoá token cũ nếu có
            tokenRepo.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(token);
            prt.setUser(user);
            prt.setExpiryDate(LocalDateTime.now().plusMinutes(30));
            tokenRepo.save(prt);

            String link = baseUrl + "/reset-password?token=" + token;
            emailService.sendResetEmail(email, link);
            return true;
        }).orElse(false);
    }

    // Đặt lại mật khẩu
    @Transactional
    public String resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token).orElse(null);
        if (prt == null) return "Token không hợp lệ";
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) return "Token đã hết hạn";

        User user = prt.getUser();
        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
        tokenRepo.delete(prt);
        return "OK";
    }
}