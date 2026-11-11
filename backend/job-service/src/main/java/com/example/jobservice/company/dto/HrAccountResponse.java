package com.example.jobservice.company.dto;

import java.util.UUID;

/**
 * Kết quả tạo tài khoản HR.
 * Mật khẩu tạm chỉ được gửi qua email cho HR, không trả về trong API response
 * vì lý do bảo mật.
 */
public record HrAccountResponse(
                UUID id,
                String email,
                String fullName,
                String role,
                UUID companyId,
                boolean mustChangePassword,
                String message) {
}
