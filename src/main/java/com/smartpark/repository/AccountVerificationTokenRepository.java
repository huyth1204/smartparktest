package com.smartpark.repository;

import com.smartpark.model.AccountVerificationToken;
import com.smartpark.model.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountVerificationTokenRepository extends JpaRepository<AccountVerificationToken, Long> {
    Optional<AccountVerificationToken> findByToken(String token);
    Optional<AccountVerificationToken> findByStaffAccount(StaffAccount staffAccount);
    void deleteByStaffAccount(StaffAccount staffAccount);
    List<AccountVerificationToken> findByExpiryDateBefore(LocalDateTime dateTime);
}
