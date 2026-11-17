package com.example.ai_career_service.dto;

import java.util.UUID;

public record RecruitmentMatchScoreResponse(UUID userId, UUID jobId, double score) {
}
