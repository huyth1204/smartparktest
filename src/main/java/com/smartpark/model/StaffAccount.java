package com.smartpark.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "staff_accounts")
@Data @NoArgsConstructor
public class StaffAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String staffCode; // NV001, AD001
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String role;   // staff / admin
    private boolean active;
    private boolean verified; // Email đã xác nhận chưa

    public StaffAccount(String staffCode, String fullName, String username, String email, String password, String role) {
        this.staffCode = staffCode;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        // Don't set active here - let business logic decide
        this.verified = false; // Mặc định chưa xác nhận
    }

    @PrePersist
    public void prePersist() {
        // Không ghi đè active - để logic business quyết định
    }
}
