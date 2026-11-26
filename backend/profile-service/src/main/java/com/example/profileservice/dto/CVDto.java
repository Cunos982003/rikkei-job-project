package com.example.profileservice.dto;

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
        private String sectionType;
        private String contentJson;
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
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCVRequest {
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        private String title;
        private Boolean isPublic;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCVSectionRequest {
        @NotNull(message = "Loại section không được để trống")
        private String sectionType;
        @NotNull(message = "Nội dung section không được để trống")
        private String contentJson;
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
}