package com.example.profileservice.profile.api.dto;

import com.example.profileservice.profile.domain.CandidateProfile;
import lombok.*;

import java.time.LocalDate;

/**
 * Payload cập nhật hồ sơ.
 *
 * <p>Mọi trường đều có thể bỏ trống: giá trị {@code null} nghĩa là "giữ nguyên",
 * tránh việc cập nhật một phần vô tình xóa các trường khác.
 *
 * <p>Các trường do LMS quản lý (fullName, email, program, classCode, courseName)
 * cố tình không có mặt ở đây để không thể ghi đè.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private LocalDate dob;
    private String gender;
    private String phone;
    private String address;
    private String avatarUrl;
    private String headline;
    private String summary;
    private CandidateProfile.Visibility visibility;
}
