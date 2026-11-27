package com.example.ai_career_service.dto.chat;

public record AiQuotaResponse(
        int used,
        int limit,
        int remaining,
        String resetAt
) {}
