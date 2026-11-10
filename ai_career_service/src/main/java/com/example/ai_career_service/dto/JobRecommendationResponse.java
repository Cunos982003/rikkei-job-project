package com.example.ai_career_service.dto;

import java.time.Instant;

public record JobRecommendationResponse(
        String jobId,
        double score,
        String explanation,
        Instant createdAt) {
}
