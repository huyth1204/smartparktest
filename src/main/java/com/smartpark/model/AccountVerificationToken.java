package com.smartpark.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "account_verification_tokens")
@Data @NoArgsConstructor
public class AccountVerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "staff_account_id")
    private StaffAccount staffAccount;

    private LocalDateTime expiryDate; // hết hạn sau 24 giờ
}
