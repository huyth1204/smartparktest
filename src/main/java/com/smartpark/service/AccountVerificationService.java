package com.smartpark.service;

import com.smartpark.model.AccountVerificationToken;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.AccountVerificationTokenRepository;
import com.smartpark.repository.StaffAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountVerificationService {

    @Autowired private AccountVerificationTokenRepository tokenRepo;
    @Autowired private StaffAccountRepository staffRepo;
    @Autowired private EmailService emailService;

    /**
     * Gửi email xác nhận cho tài khoản mới
     */
    @Transactional
    public void sendVerificationEmail(StaffAccount account, String baseUrl, String tempPassword) {
        // Xóa token cũ nếu có
        tokenRepo.deleteByStaffAccount(account);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setStaffAccount(account);
        vt.setExpiryDate(LocalDateTime.now().plusHours(24)); // Hết hạn sau 24 giờ
        tokenRepo.save(vt);

        // Gửi email
        String verifyLink = baseUrl + "/verify-account?token=" + token;
        emailService.sendAccountVerificationEmail(account.getEmail(), verifyLink, account.getUsername(), tempPassword);
    }

    /**
     * Xác nhận tài khoản bằng token
     */
    @Transactional
    public String verifyAccount(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) return "Token đã hết hạn";

        StaffAccount account = vt.getStaffAccount();
        account.setVerified(true);
        account.setActive(true);
        staffRepo.save(account);
        tokenRepo.delete(vt);
        
        return "OK";
    }
}
