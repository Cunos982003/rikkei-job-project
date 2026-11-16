package com.example.jobservice.company.dto;

import java.util.UUID;

/**
 * Kết quả tạo tài khoản HR.
 */
public record HrAccountResponse(
        UUID id,
        String email,
        String fullName,
        String role,
        UUID companyId,
        boolean mustChangePassword,
        String temporaryPassword,
        boolean emailSent,
        String message
) {
    public HrAccountResponse(
            UUID id,
            String email,
            String fullName,
            String role,
            UUID companyId,
            boolean mustChangePassword,
            String message) {
        this(id, email, fullName, role, companyId, mustChangePassword, null, true, message);
    }
}
