package com.example.profileservice.internship.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PmInternshipMockDto(
        String batchId, // Đổi từ Long sang String
        String batchName,
        String groupName,
        String topicName,
        String mentorEmail,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal finalScore,
        List<PmTaskMockDto> tasks
) {
    public record PmTaskMockDto(String title, BigDecimal acceptedScore, String acceptedAt) {}
}