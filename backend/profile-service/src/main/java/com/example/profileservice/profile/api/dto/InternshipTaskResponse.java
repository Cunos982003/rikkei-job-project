package com.example.profileservice.internship.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InternshipTaskResponse(
        UUID id, // Đổi từ Long sang UUID
        String title,
        BigDecimal acceptedScore,
        OffsetDateTime acceptedAt
) {}