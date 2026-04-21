package com.smartpark.service;

import com.smartpark.model.StaffAccount;
import java.util.List;
import java.util.Optional;

/**
 * Service interface quản lý tài khoản nhân viên.
 */
public interface AccountService {

    Optional<StaffAccount> authenticate(String username, String password);

    List<StaffAccount> getAllAccounts();

    StaffAccount createAccount(String fullName, String username, String password, String role);

    StaffAccount updateAccount(Long id, String fullName, String role, boolean active, String password);

    void deleteAccount(Long id);

    // Password Reset methods
    boolean sendStaffResetLink(String email, String baseUrl);
    
    String resetStaffPassword(String token, String newPassword);
}
