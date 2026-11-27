package com.example.profileservice.dto;

import com.example.profileservice.entity.SectionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CVDto {

    private UUID id;
    private String title;
    private String targetPosition;
    private String template;
    private String publicToken;
    private boolean isPublic;
    private String pdfUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<CVSectionDto> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CVSectionDto {
        private UUID id;
        private SectionType sectionType;
        private List<UUID> itemIds;
        private boolean included;
        private int sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCVRequest {
        @NotBlank(message = "Tiêu đề CV không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        private String title;

        @Size(max = 200, message = "Vị trí mục tiêu tối đa 200 ký tự")
        private String targetPosition;

        private String template;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCVRequest {
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        private String title;

        @Size(max = 200, message = "Vị trí mục tiêu tối đa 200 ký tự")
        private String targetPosition;

        private String template;
        private Boolean isPublic;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCVSectionRequest {
        @NotNull(message = "Loại section không được để trống")
        private SectionType sectionType;

        private List<UUID> itemIds;

        private Boolean included;

        private Integer sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReorderCVSectionsRequest {
        @NotNull @Size(min = 1)
        private List<UUID> sectionIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneratePDFResponse {
        private String pdfUrl;
        private String publicUrl;
    }

    // Preview DTOs - reads live data from profile tables
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CVPreviewResponse {
        private UUID id;
        private String title;
        private String targetPosition;
        private String template;
        private String publicToken;
        private boolean isPublic;
        private List<SectionPreviewData> sections;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SectionPreviewData {
        private SectionType sectionType;
        private boolean included;
        private int sortOrder;
        private Object data; // Dynamic based on section type
    }

    // Section-specific preview data classes
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonalInfoPreview {
        private String fullName;
        private String email;
        private String phone;
        private String address;
        private String dob;
        private String gender;
        private String headline;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EducationPreview {
        private UUID id;
        private String school;
        private String major;
        private String fromDate;
        private String toDate;
        private String gpa;
        private String note;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectPreview {
        private UUID id;
        private String name;
        private String role;
        private String description;
        private String techStack;
        private String repoUrl;
        private String demoUrl;
        private String startDate;
        private String endDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CertificatePreview {
        private UUID id;
        private String name;
        private String issuer;
        private String issuedAt;
        private String expiresAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillPreview {
        private UUID skillId;
        private String skillName;
        private String level;
        private Short verifiedScore;
        private Short selfScore;
        private String verificationStatus; // "Đã xác thực" or "Tự khai"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipPreview {
        private UUID id;
        private String batchName;
        private String topicName;
        private String groupName;
        private String startDate;
        private String endDate;
        private String finalScore;
        private List<InternshipTaskPreview> tasks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipTaskPreview {
        private String title;
        private String acceptedScore;
        private String acceptedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryPreview {
        private String summary;
    }
}