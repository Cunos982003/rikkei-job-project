package com.example.profileservice.profile.api.dto;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.skill.domain.SkillEvidence;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPreviewProfileDto {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String maskedEmail;
    private String maskedPhone;
    private String dob;
    private String gender;
    private String address;
    private String avatarUrl;
    private String headline;
    private String summary;
    private String program;
    private String classCode;
    private String courseName;
    private Visibility visibility;
    private OffsetDateTime consentAt;
    private OffsetDateTime consentWithdrawnAt;
    private OffsetDateTime updatedAt;
    private List<SkillProfileDto> skills;
    private List<EducationDto> educations;
    private List<CertificateDto> certificates;
    private List<ReviewHistoryDto> reviewHistory;
    private int rejectionCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillProfileDto {
        private UUID skillId;
        private String skillCode;
        private String skillName;
        private String group;
        private Integer verifiedScore;
        private Integer selfScore;
        private String level;
        private List<SkillEvidenceDto> evidences;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SkillEvidenceDto {
            private String source;
            private String sourceRef;
            private String title;
            private Integer score;
            private OffsetDateTime recordedAt;
            private Boolean verified;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationDto {
        private UUID id;
        private String school;
        private String major;
        private String fromDate;
        private String toDate;
        private Double gpa;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateDto {
        private UUID id;
        private String name;
        private String issuer;
        private String issuedAt;
        private String expiresAt;
        private String fileUrl;
        private String fileType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewHistoryDto {
        private UUID id;
        private String action;
        private String reason;
        private UUID actorId;
        private OffsetDateTime createdAt;
    }
}