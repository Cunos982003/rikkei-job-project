package com.example.ai_career_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobRecommendationResponse(
        UUID jobId,
        String jobTitle,
        String companyName,
        double score,
        boolean belowMinimum,
        String explanation,
        Instant createdAt,
        List<String> missingRequiredSkills,
        List<SkillGapResponse> skillGaps) {

    public JobRecommendationResponse(
            UUID jobId,
            String jobTitle,
            String companyName,
            double score,
            String explanation,
            Instant createdAt,
            List<String> missingRequiredSkills,
            List<SkillGapResponse> skillGaps) {
        this(jobId, jobTitle, companyName, score, false, explanation, createdAt, missingRequiredSkills, skillGaps);
    }

    public JobRecommendationResponse(
            UUID jobId,
            double score,
            String explanation,
            Instant createdAt,
            List<String> missingRequiredSkills,
            List<SkillGapResponse> skillGaps) {
        this(jobId, null, null, score, false, explanation, createdAt, missingRequiredSkills, skillGaps);
    }
}
