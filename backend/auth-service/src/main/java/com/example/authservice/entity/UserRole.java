package com.example.authservice.entity;

/**
 * Định nghĩa vai trò người dùng trong hệ thống Rikkei Jobs Platform.
 * Đồng nhất 100% với Mock Server và Frontend:
 *  - CANDIDATE: Học viên / Ứng viên tìm việc
 *  - HR: Nhà tuyển dụng doanh nghiệp
 *  - MENTOR: Giảng viên / Cố vấn chuyên môn
 *  - STAFF: Cán bộ tuyển sinh & quan hệ doanh nghiệp
 *  - ADMIN: Quản trị viên hệ thống
 */
public enum UserRole {
    CANDIDATE,
    HR,
    MENTOR,
    STAFF,
    ADMIN;

    /**
     * Trả về định dạng Authority của Spring Security (bắt đầu bằng ROLE_)
     */
    public String withPrefix() {
        return "ROLE_" + this.name();
    }

    /**
     * Chuyển đổi an toàn từ chuỗi (hỗ trợ cả tiền tố ROLE_ và role STUDENT từ LMS)
     */
    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) {
            return CANDIDATE;
        }
        String clean = value.trim().toUpperCase();
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        if ("STUDENT".equals(clean)) {
            return CANDIDATE;
        }
        try {
            return UserRole.valueOf(clean);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Không tìm thấy vai trò phù hợp cho: " + value);
        }
    }
}

