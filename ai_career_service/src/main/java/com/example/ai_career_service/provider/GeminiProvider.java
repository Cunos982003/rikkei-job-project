package com.example.ai_career_service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiProvider extends AbstractHttpAIProvider {

    public GeminiProvider(ObjectMapper objectMapper, String apiKey, String endpoint) {
        super(objectMapper, apiKey, endpoint);
    }

    @Override
    protected HttpRequest createRequest(String prompt) throws IOException {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 180));
        return requestBuilder(endpoint())
                .header("x-goog-api-key", apiKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(json(body)))
                .build();
    }

    @Override
    protected HttpRequest createChatRequest(String systemPrompt, String userPrompt) throws IOException {
        Map<String, Object> body = new HashMap<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))));
        body.put("generationConfig", Map.of("maxOutputTokens", 1024));

        return chatRequestBuilder(endpoint())
                .header("x-goog-api-key", apiKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(json(body)))
                .build();
    }

    @Override
    protected String extractText(JsonNode response) {
        return requiredText(response, "/candidates/0/content/parts/0/text");
    }
}
