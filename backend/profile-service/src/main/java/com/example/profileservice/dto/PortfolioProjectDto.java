package com.example.profileservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioProjectDto {

    private UUID id;

    @NotBlank(message = "Tên dự án không được để trống")
    @Size(max = 300, message = "Tên dự án tối đa 300 ký tự")
    private String name;

    @Size(max = 300, message = "Vai trò tối đa 300 ký tự")
    private String role;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 500, message = "Công nghệ tối đa 500 ký tự")
    private String techStack;

    @Size(max = 500, message = "URL repository tối đa 500 ký tự")
    private String repoUrl;

    @Size(max = 500, message = "URL demo tối đa 500 ký tự")
    private String demoUrl;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private boolean visible = true;

    @Builder.Default
    private int sortOrder = 0;

    private UUID internshipId;

    private String source;

    private String sourceRef;

    private List<PortfolioMediaDto> media;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PortfolioMediaDto {
        private UUID id;
        private String url;
        private String mediaType;
        private int sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReorderProjectsRequest {
        @NotNull(message = "Danh sách ID dự án không được để trống")
        @Size(min = 1, message = "Phải có ít nhất 1 dự án")
        private List<UUID> projectIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReorderMediaRequest {
        @NotNull(message = "Danh sách ID ảnh không được để trống")
        @Size(min = 1, message = "Phải có ít nhất 1 ảnh")
        private List<UUID> mediaIds;
    }
}