package com.example.profileservice.client;

import com.example.profileservice.dto.LmsStudentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.empty())
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    HttpStatusCode statusCode = response.statusCode();
                                    HttpStatus httpStatus = HttpStatus.valueOf(statusCode.value());
                                    return Mono.error(new LmsClientException(
                                            "LMS request failed: " + statusCode + " - " + body,
                                            httpStatus));
                                }))
                .bodyToMono(LmsStudentDto.class);
    }

    public static class LmsClientException extends RuntimeException {
        private final HttpStatus statusCode;

        public LmsClientException(String message, HttpStatus statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public HttpStatus getStatusCode() {
            return statusCode;
        }
    }
}