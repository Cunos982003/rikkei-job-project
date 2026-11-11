package com.example.ai_career_service.provider;

import com.example.ai_career_service.dto.prompt.RecommendationPrompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

abstract class AbstractHttpAIProvider implements AIProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String EXPLANATION_SYSTEM_PROMPT =
            "Viết giải thích ngắn, chính xác bằng tiếng Việt.";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String endpoint;

    AbstractHttpAIProvider(ObjectMapper objectMapper, String apiKey, String endpoint) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public final String generateExplanation(RecommendationPrompt prompt) {
        return generateText(EXPLANATION_SYSTEM_PROMPT, prompt.toUserPrompt(), 180);
    }

    @Override
    public final String generateDraft(String prompt) {
        return generateText(
                "Bạn là trợ lý viết CV. Chỉ sử dụng dữ kiện có trong dữ liệu đầu vào. "
                        + "Không suy đoán hoặc thêm thông tin. Trả về duy nhất JSON hợp lệ với dạng "
                        + "{\"content\":\"nội dung nháp\",\"skills\":[\"mọi tên kỹ năng/công nghệ "
                        + "được nhắc trong nội dung\"]}.",
                prompt,
                700);
    }

    private String generateText(String systemPrompt, String userPrompt, int maxOutputTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("The selected AI provider API key is not configured");
        }
        try {
            HttpRequest request = createRequest(systemPrompt, userPrompt, maxOutputTokens);
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI provider returned HTTP " + response.statusCode());
            }
            return extractText(objectMapper.readTree(response.body()));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not communicate with the selected AI provider", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI provider request was interrupted", exception);
        }
    }

    protected abstract HttpRequest createRequest(
            String systemPrompt,
            String userPrompt,
            int maxOutputTokens) throws IOException;

    protected abstract String extractText(JsonNode response);

    protected final String endpoint() {
        return endpoint;
    }

    protected final String apiKey() {
        return apiKey;
    }

    protected final ObjectMapper objectMapper() {
        return objectMapper;
    }

    protected final HttpRequest.Builder requestBuilder(String endpoint) {
        return HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json");
    }

    protected final byte[] json(Map<String, Object> body) throws IOException {
        return objectMapper.writeValueAsBytes(body);
    }

    protected final String requiredText(JsonNode node, String path) {
        JsonNode text = node.at(path);
        if (!text.isTextual() || text.asText().isBlank()) {
            throw new IllegalStateException("AI provider returned an empty explanation");
        }
        return text.asText();
    }
}
