package com.smartpark.service;

import com.smartpark.model.AccountVerificationToken;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.AccountVerificationTokenRepository;
import com.smartpark.repository.StaffAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountVerificationService {

    @Autowired private AccountVerificationTokenRepository tokenRepo;
    @Autowired private StaffAccountRepository staffRepo;
    @Autowired private EmailService emailService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    /**
     * Gửi email xác nhận cho tài khoản mới (không gửi password)
     */
    @Transactional
    public void sendVerificationEmail(StaffAccount account, String baseUrl) {
        // Xóa token cũ nếu có
        tokenRepo.deleteByStaffAccount(account);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setStaffAccount(account);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút
        tokenRepo.save(vt);

        // Gửi email
        String verifyLink = baseUrl + "/verify-account?token=" + token;
        emailService.sendAccountVerificationEmail(account.getEmail(), verifyLink, account.getUsername());
    }

    /**
     * Xác nhận token hợp lệ (không kích hoạt tài khoản ngay)
     */
    @Transactional
    public String verifyAccount(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Token hết hạn - xóa token và tài khoản chưa xác minh
            StaffAccount account = vt.getStaffAccount();
            tokenRepo.delete(vt);
            if (!account.isVerified()) {
                staffRepo.delete(account);
            }
            return "Token đã hết hạn";
        }
        
        return "OK";
    }

    /**
     * Validate token (dùng cho GET /set-password)
     */
    public String validateToken(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "Token đã hết hạn";
        }
        return "OK";
    }

    /**
     * Đặt mật khẩu và kích hoạt tài khoản
     */
    @Transactional
    public String setPassword(String token, String newPassword) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) return "Token đã hết hạn";

        StaffAccount account = vt.getStaffAccount();
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerified(true);
        account.setActive(true);
        staffRepo.save(account);
        tokenRepo.delete(vt);
        
        return "OK";
    }

    /**
     * Kiểm tra token có hết hạn không
     */
    public boolean isTokenExpired(StaffAccount account) {
        return tokenRepo.findByStaffAccount(account)
            .map(token -> token.getExpiryDate().isBefore(LocalDateTime.now()))
            .orElse(true);
    }

    /**
     * Scheduled job: Xóa tokens hết hạn và tài khoản chưa xác minh
     * Chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 phút = 300,000 ms
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<AccountVerificationToken> expiredTokens = tokenRepo.findByExpiryDateBefore(now);
        
        for (AccountVerificationToken token : expiredTokens) {
            StaffAccount account = token.getStaffAccount();
            tokenRepo.delete(token);
            
            // Xóa tài khoản nếu chưa được xác minh
            if (!account.isVerified()) {
                staffRepo.delete(account);
            }
        }
    }
}
