package com.example.recruitment_service.dto;

import java.util.UUID;

public record EvaluationFormResponse(
        UUID evaluationId,
        String status,       // DRAFT | SUBMITTED
        boolean readOnly,    // true nếu đã gửi
        String internName,
        String batchName
) {}