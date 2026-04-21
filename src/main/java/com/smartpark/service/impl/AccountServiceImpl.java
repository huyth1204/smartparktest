package com.smartpark.service.impl;

import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

    private final StaffAccountRepository staffRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    public AccountServiceImpl(StaffAccountRepository staffRepo, BCryptPasswordEncoder passwordEncoder) {
        this.staffRepo       = staffRepo;
        this.passwordEncoder = passwordEncoder;
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
        if (!staffRepo.existsById(id))
            throw new ResourceNotFoundException("Tài khoản", "id", id);
        staffRepo.deleteById(id);
    }
}
