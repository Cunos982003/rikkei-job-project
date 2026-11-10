package com.example.jobservice.company.dto;

/**
 * Kết quả tạo tài khoản HR.
 * Mật khẩu tạm chỉ được gửi qua email cho HR, không trả về trong API response vì lý do bảo mật.
 */
public record HrAccountResponse(
        Long id,
        String email,
        String fullName,
        String role,
        Long companyId,
        boolean mustChangePassword,
        String message) {
}
