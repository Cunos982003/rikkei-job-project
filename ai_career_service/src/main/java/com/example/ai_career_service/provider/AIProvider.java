package com.example.ai_career_service.provider;

import com.example.ai_career_service.dto.prompt.RecommendationPrompt;

public interface AIProvider {
    String generateExplanation(
            RecommendationPrompt prompt);

    default String chat(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("chat() not implemented for " + getClass().getSimpleName());
    }
}
