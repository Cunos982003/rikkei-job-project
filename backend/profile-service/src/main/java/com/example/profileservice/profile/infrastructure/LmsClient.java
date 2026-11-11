package com.example.profileservice.profile.infrastructure;

import com.example.profileservice.profile.api.dto.LmsStudentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LmsClient {

    private final WebClient webClient;

    public LmsClient(
            @Value("${app.lms.base-url:http://localhost:9000}") String baseUrl,
            @Value("${app.lms.service-key:dev-key}") String serviceKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    public Mono<LmsStudentDto> getStudent(UUID userId) {
        return webClient.get()
                .uri("/lms/students/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new LmsClientException(
                                        "LMS request failed: " + response.statusCode() + " - " + body))))
                .bodyToMono(LmsStudentDto.class);
    }

    public static class LmsClientException extends RuntimeException {
        public LmsClientException(String message) {
            super(message);
        }
    }
}