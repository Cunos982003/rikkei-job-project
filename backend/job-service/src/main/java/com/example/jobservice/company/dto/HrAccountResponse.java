package com.example.jobservice.company.dto;

import java.util.UUID;

/**
 * HR account creation result, including the outcome of post-commit email delivery.
 */
public class HrAccountResponse {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String role;
    private final UUID companyId;
    private final boolean mustChangePassword;
    private boolean emailSent;
    private String message;

    public HrAccountResponse(
            UUID id,
            String email,
            String fullName,
            String role,
            UUID companyId,
            boolean mustChangePassword) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.companyId = companyId;
        this.mustChangePassword = mustChangePassword;
        this.message = "Tài khoản đã được tạo; đang gửi email thông tin đăng nhập.";
    }

    public void markEmailSent() {
        emailSent = true;
        message = "Tạo tài khoản HR thành công. Thông tin đăng nhập đã được gửi tới email " + email;
    }

    public void markEmailFailed() {
        emailSent = false;
        message = "Tạo tài khoản HR thành công nhưng không gửi được email. Vui lòng kiểm tra cấu hình email và thử gửi lại.";
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public String getMessage() {
        return message;
    }
}
