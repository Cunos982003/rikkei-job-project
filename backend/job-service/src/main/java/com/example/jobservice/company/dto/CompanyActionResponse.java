package com.example.jobservice.company.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyActionResponse(
        UUID id,
        String action,
        UUID targetId,
        String targetTitle,
        String reason,
        String staffId,
        OffsetDateTime createdAt
) {}

