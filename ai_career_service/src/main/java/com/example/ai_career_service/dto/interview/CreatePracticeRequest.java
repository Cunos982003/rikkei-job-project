package com.example.ai_career_service.dto.interview;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePracticeRequest(
        @NotNull(message = "jobId không được để trống")
        UUID jobId
) {}
