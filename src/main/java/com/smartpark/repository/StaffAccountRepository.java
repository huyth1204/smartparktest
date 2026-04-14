package com.smartpark.repository;

import com.smartpark.model.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {
    Optional<StaffAccount> findByUsername(String username);
    List<StaffAccount> findAllByOrderByStaffCodeAsc();
    boolean existsByUsernameIgnoreCase(String username);
}
