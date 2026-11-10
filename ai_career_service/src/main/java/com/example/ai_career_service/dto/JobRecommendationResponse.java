package com.example.ai_career_service.dto;

import java.time.Instant;
import java.util.List;

public record JobRecommendationResponse(
        String jobId,
        double score,
        String explanation,
        Instant createdAt,
        List<String> missingRequiredSkills,
        List<SkillGapResponse> skillGaps) {
}
