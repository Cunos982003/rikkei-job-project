package com.example.authservice.client.lms;

import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class LmsClient {

    private static final Logger log = LoggerFactory.getLogger(LmsClient.class);

    private final RestClient restClient;
    private final String verifyEndpoint;
    private final int maxRetries;

    public LmsClient(
        @Value("${app.lms.base-url:http://localhost:9000}") String baseUrl,
        @Value("${app.lms.service-key:mock-lms-service-key-2026}") String serviceKey,
        @Value("${app.lms.verify-endpoint:/lms/sso/verify}") String verifyEndpoint,
        @Value("${app.lms.timeout-seconds:5}") int timeoutSeconds,
        @Value("${app.lms.max-retries:2}") int maxRetries
    ) {
        this.verifyEndpoint = verifyEndpoint;
        this.maxRetries = maxRetries;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("X-Service-Key", serviceKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public LmsUserResponse verifyTicket(String ticket) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return executeVerify(ticket);
            } catch (ResourceAccessException ex) {
                log.warn("Network error contacting Mock LMS (attempt {}/{}): {}", attempts, maxRetries + 1, ex.getMessage());
                if (attempts > maxRetries) {
                    throw new SsoException(ErrorCode.EXTERNAL_UNAVAILABLE, "Mock LMS service is unavailable or timed out", ex);
                }
                try {
                    Thread.sleep(100L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SsoException(ErrorCode.EXTERNAL_UNAVAILABLE, "LMS request interrupted", ie);
                }
            }
        }
    }

    private LmsUserResponse executeVerify(String ticket) {
        return restClient.post()
            .uri(verifyEndpoint)
            .body(new LmsVerifyRequest(ticket))
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, resp) -> {
                throw new SsoException(ErrorCode.TICKET_INVALID, "Ticket not found or invalid in Mock LMS");
            })
            .onStatus(status -> status.value() == 503 || status.value() == 504, (req, resp) -> {
                throw new SsoException(ErrorCode.EXTERNAL_UNAVAILABLE, "Mock LMS service is unavailable or timed out");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                throw new SsoException(ErrorCode.TICKET_INVALID, "Mock LMS rejected the ticket with status " + resp.getStatusCode());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                throw new SsoException(ErrorCode.LMS_ERROR, "Mock LMS server responded with error " + resp.getStatusCode());
            })
            .body(LmsUserResponse.class);
    }
}
