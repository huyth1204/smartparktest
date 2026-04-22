package com.smartpark.service.impl;

import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.model.PasswordResetToken;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.PasswordResetTokenRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountService;
import com.smartpark.service.EmailService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final StaffAccountRepository staffRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final com.smartpark.repository.AccountVerificationTokenRepository verificationTokenRepo;

    public AccountServiceImpl(StaffAccountRepository staffRepo, 
                             BCryptPasswordEncoder passwordEncoder,
                             PasswordResetTokenRepository tokenRepo,
                             EmailService emailService,
                             com.smartpark.repository.AccountVerificationTokenRepository verificationTokenRepo) {
        this.staffRepo       = staffRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepo       = tokenRepo;
        this.emailService    = emailService;
        this.verificationTokenRepo = verificationTokenRepo;
    }

    @Override
    public Optional<StaffAccount> authenticate(String username, String password) {
        return staffRepo.findByUsername(username)
                .filter(acc -> passwordEncoder.matches(password, acc.getPassword()));
    }

    @Override
    public List<StaffAccount> getAllAccounts() {
        return staffRepo.findAllByOrderByStaffCodeAsc();
    }

    @Override
    public StaffAccount createAccount(String fullName, String username,
                                       String password, String role) {
        long count = staffRepo.count();
        String prefix = "admin".equals(role) ? "AD" : "NV";
        StaffAccount acc = new StaffAccount(
                prefix + String.format("%03d", count + 1),
                fullName, username, null, passwordEncoder.encode(password), role
        );
        acc.setVerified(true);
        acc.setActive(true);
        return staffRepo.save(acc);
    }

    @Override
    public StaffAccount updateAccount(Long id, String fullName, String role,
                                       boolean active, String password) {
        StaffAccount acc = staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", id));
        acc.setFullName(fullName);
        acc.setRole(role);
        acc.setActive(active);
        if (password != null && !password.isBlank()) {
            acc.setPassword(passwordEncoder.encode(password));
        }
        return staffRepo.save(acc);
    }

    @Override
    public void deleteAccount(Long id) {
        staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", id));
        staffRepo.deleteById(id);
    }

    @Override
    @Transactional
    public boolean sendStaffResetLink(String email, String baseUrl) {
        return staffRepo.findByEmail(email).map(staff -> {
            // Xóa token cũ nếu có
            tokenRepo.deleteByStaffAccount(staff);

            // Tạo token mới
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(token);
            prt.setStaffAccount(staff);
            prt.setExpiryDate(LocalDateTime.now().plusMinutes(30));
            tokenRepo.save(prt);

            // Gửi email
            String link = baseUrl + "/staff/reset-password?token=" + token;
            emailService.sendStaffResetEmail(email, staff.getFullName(), link);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public String resetStaffPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token).orElse(null);
        if (prt == null) return "Token không hợp lệ";
        if (prt.getStaffAccount() == null) return "Token không dành cho nhân viên";
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) return "Token đã hết hạn";

        StaffAccount staff = prt.getStaffAccount();
        staff.setPassword(passwordEncoder.encode(newPassword));
        staffRepo.save(staff);
        tokenRepo.delete(prt);
        return "OK";
    }
}
