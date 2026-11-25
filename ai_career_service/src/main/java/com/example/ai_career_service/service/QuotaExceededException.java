package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.chat.AiQuotaResponse;
import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {
    private final AiQuotaResponse quota;

    public QuotaExceededException(String message, AiQuotaResponse quota) {
        super(message);
        this.quota = quota;
    }
}
