package com.example.ai_career_service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

public class ClaudeProvider extends AbstractHttpAIProvider {

    private final String model;

    public ClaudeProvider(ObjectMapper objectMapper, String apiKey, String endpoint, String model) {
        super(objectMapper, apiKey, endpoint);
        this.model = model;
    }

    @Override
    protected HttpRequest createRequest(String prompt) throws IOException {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 180,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        return requestBuilder(endpoint())
                .header("x-api-key", apiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json(body)))
                .build();
    }

    @Override
    protected String extractText(JsonNode response) {
        return requiredText(response, "/content/0/text");
    }
}
