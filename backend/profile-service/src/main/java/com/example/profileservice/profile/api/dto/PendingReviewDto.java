package com.example.profileservice.profile.api.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingReviewDto {
    private UUID userId;
    private String fullName;
    private String email;
    private String headline;
    private String avatarUrl;
    private OffsetDateTime submittedAt;
}
