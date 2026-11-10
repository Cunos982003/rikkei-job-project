package com.example.ai_career_service.provider;

import com.example.ai_career_service.dto.prompt.RecommendationPrompt;

public class MockProvider implements AIProvider {
    @Override
    public String generateExplanation(
            RecommendationPrompt prompt) {

        return com.example.ai_career_service.service.RecommendationExplanation.fallback(prompt);
    }
}
