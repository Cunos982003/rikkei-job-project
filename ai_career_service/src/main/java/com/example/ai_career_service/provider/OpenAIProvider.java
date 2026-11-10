package com.example.ai_career_service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

public class OpenAIProvider extends AbstractHttpAIProvider {

    private final String model;

    public OpenAIProvider(ObjectMapper objectMapper, String apiKey, String endpoint, String model) {
        super(objectMapper, apiKey, endpoint);
        this.model = model;
    }

    @Override
    protected HttpRequest createRequest(String prompt) throws IOException {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "Viết giải thích ngắn, chính xác bằng tiếng Việt."),
                        Map.of("role", "user", "content", prompt)),
                "max_tokens", 180);
        return requestBuilder(endpoint())
                .header("Authorization", "Bearer " + apiKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(json(body)))
                .build();
    }

    @Override
    protected String extractText(JsonNode response) {
        return requiredText(response, "/choices/0/message/content");
    }
}
