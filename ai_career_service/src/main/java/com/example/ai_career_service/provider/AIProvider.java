package com.example.ai_career_service.provider;

import com.example.ai_career_service.dto.prompt.RecommendationPrompt;

public interface AIProvider {
    String generateExplanation(
            RecommendationPrompt prompt);
}
