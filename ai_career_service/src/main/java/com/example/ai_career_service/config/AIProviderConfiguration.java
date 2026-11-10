package com.example.ai_career_service.config;

import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.provider.ClaudeProvider;
import com.example.ai_career_service.provider.GeminiProvider;
import com.example.ai_career_service.provider.MockProvider;
import com.example.ai_career_service.provider.OpenAIProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIProviderConfiguration {

    @Bean
    ObjectMapper aiObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService aiExecutor() {
        return Executors.newFixedThreadPool(5);
    }

    @Bean
    AIProvider aiProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.provider:mock}") String provider,
            @Value("${app.ai.openai.api-key:}") String openAiKey,
            @Value("${app.ai.openai.endpoint:https://api.openai.com/v1/chat/completions}") String openAiEndpoint,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String openAiModel,
            @Value("${app.ai.gemini.api-key:}") String geminiKey,
            @Value("${app.ai.gemini.endpoint:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}") String geminiEndpoint,
            @Value("${app.ai.claude.api-key:}") String claudeKey,
            @Value("${app.ai.claude.endpoint:https://api.anthropic.com/v1/messages}") String claudeEndpoint,
            @Value("${app.ai.claude.model:claude-3-5-haiku-latest}") String claudeModel) {
        return switch (provider.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "mock" -> new MockProvider();
            case "openai" -> new OpenAIProvider(objectMapper, openAiKey, openAiEndpoint, openAiModel);
            case "gemini" -> new GeminiProvider(objectMapper, geminiKey, geminiEndpoint);
            case "claude" -> new ClaudeProvider(objectMapper, claudeKey, claudeEndpoint, claudeModel);
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + provider);
        };
    }
}
