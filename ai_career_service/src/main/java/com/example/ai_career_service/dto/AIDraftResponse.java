package com.example.ai_career_service.dto;

import com.example.ai_career_service.enums.AIDraftSection;
import com.example.ai_career_service.enums.AIDraftStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AIDraftResponse(
        UUID id,
        UUID cvId,
        AIDraftSection section,
        UUID projectId,
        UUID targetJobId,
        String output,
        List<String> warnings,
        AIDraftStatus status,
        Instant createdAt) {
}
