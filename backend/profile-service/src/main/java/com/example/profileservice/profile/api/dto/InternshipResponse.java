package com.example.profileservice.internship.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InternshipResponse(
        UUID id,             // Đổi từ Long sang UUID
        String batchName,
        String groupName,
        String topicName,
        String mentorEmail,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal finalScore,
        OffsetDateTime syncedAt,
        List<InternshipTaskResponse> tasks
) {}