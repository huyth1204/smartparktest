package com.smartpark.dto.response;

import com.smartpark.model.StaffAccount;

/**
 * DTO trả về thông tin tài khoản — KHÔNG chứa password.
 * Thể hiện DTO Pattern: tách biệt Entity (DB) và data trả về client.
 */
public record StaffAccountResponse(
        Long id,
        String staffCode,
        String fullName,
        String username,
        String role,
        boolean active
) {
    /** Factory method: chuyển Entity → DTO */
    public static StaffAccountResponse from(StaffAccount entity) {
        return new StaffAccountResponse(
                entity.getId(),
                entity.getStaffCode(),
                entity.getFullName(),
                entity.getUsername(),
                entity.getRole(),
                entity.isActive()
        );
    }
}
