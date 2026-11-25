package com.example.profileservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ExternalMockClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final int maxRetries;

    private static final Map<String, String> KNOWN_UUID_TO_STUDENT = Map.of(
            "ed854c07-f3eb-48f1-a186-4e7135f54645", "std-001",
            "797c5663-386b-4e96-9112-6d3e414b4449", "std-002",
            "4a09b051-d7ab-40a5-8322-963ec9ebdbd7", "std-003"
    );

    public ExternalMockClient(
            @Value("${app.sync.mock-base-url:${app.lms.base-url:http://localhost:9000}}") String baseUrl,
            @Value("${app.sync.service-key:fbc368ffa97a999a71577c552ecc2546815f245fd4ebee014732afcba1b52870}") String serviceKey,
            @Value("${app.sync.timeout-seconds:5}") int timeoutSeconds,
            @Value("${app.sync.max-retries:2}") int maxRetries,
            ObjectMapper objectMapper) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.maxRetries = maxRetries;
        this.objectMapper = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String resolveStudentId(UUID userId, String email) {
        if (userId != null && KNOWN_UUID_TO_STUDENT.containsKey(userId.toString())) {
            return KNOWN_UUID_TO_STUDENT.get(userId.toString());
        }
        if (email != null && email.contains("@")) {
            String prefix = email.split("@")[0].toLowerCase();
            if (prefix.startsWith("candidate")) {
                try {
                    int num = Integer.parseInt(prefix.replace("candidate", ""));
                    return String.format("std-%03d", num);
                } catch (NumberFormatException ignored) {}
            }
        }
        return "std-001";
    }

    public JsonNode getLmsResults(String studentId) {
        return fetchJson("/lms/students/{studentId}/results", studentId);
    }

    public JsonNode getExamResults(String studentId) {
        return fetchJson("/exam/students/{studentId}/results", studentId);
    }

    public JsonNode getPmInternships(String studentId) {
        return fetchJson("/pm/students/{studentId}/internships", studentId);
    }

    private JsonNode fetchJson(String path, Object... uriVariables) {
        try {
            String body = webClient.get()
                    .uri(path, uriVariables)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofMillis(500))
                            .filter(throwable -> !(throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException.NotFound))
                            .doBeforeRetry(retrySignal -> log.warn("Retrying external call {} (attempt {}) due to {}", path, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                    .block();

            if (body == null || body.isBlank()) {
                return objectMapper.createArrayNode();
            }
            return objectMapper.readTree(body);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound notFound) {
            log.info("Endpoint {} returned 404 Not Found for parameters: {}", path, uriVariables);
            return objectMapper.createArrayNode();
        } catch (Exception e) {
            log.error("Failed to fetch data from mock endpoint {} with variables {}: {}", path, uriVariables, e.getMessage());
            throw new ExternalServiceException("Failed to call external system " + path + ": " + e.getMessage(), e);
        }
    }

    public static class ExternalServiceException extends RuntimeException {
        public ExternalServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
