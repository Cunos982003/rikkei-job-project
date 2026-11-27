package com.example.ai_career_service.dto;

import com.example.ai_career_service.enums.AIDraftSection;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AIDraftRequest(
        @NotNull AIDraftSection section,
        UUID projectId,
        UUID jobId) {
}
