package com.example.ai_career_service.dto.chat;

public record SendMessageResponse(
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage,
        AiQuotaResponse quota
) {}
