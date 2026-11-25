package com.example.profileservice.dto;

import com.example.profileservice.entity.ContactRequest;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactRequestDto {
    private UUID id;
    private UUID companyId;
    private UUID userId;
    private UUID jobId;
    private String reason;
    private ContactRequest.ContactRequestStatus status;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // For HR create request
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        private UUID jobId;
        private String reason;
    }

    // For STAFF reject
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RejectRequest {
        private String reason;
    }

    public static ContactRequestDto from(ContactRequest e) {
        return ContactRequestDto.builder()
                .id(e.getId())
                .companyId(e.getCompanyId())
                .userId(e.getUserId())
                .jobId(e.getJobId())
                .reason(e.getReason())
                .status(e.getStatus())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}