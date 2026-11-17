package com.example.recruitment_service.integration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.recruitment_service.dto.RecruitmentMatchScoreResponse;
import com.example.recruitment_service.exception.ApiException;

@Component
public class AiCareerServiceClient {
    private static final String MATCH_SCORE_PATH =
            "/internal/v1/recruitments/{userId}/jobs/{jobId}/match-score";

    private final RestClient restClient;
    private final String internalServiceToken;

    @Autowired
    public AiCareerServiceClient(
            @Value("${app.integrations.ai-career-service-url}") String baseUrl,
            @Value("${app.integrations.internal-service-token:}") String internalServiceToken) {
        this(baseUrl, internalServiceToken, RestClient.builder());
    }

    AiCareerServiceClient(String baseUrl, String internalServiceToken, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalServiceToken = internalServiceToken;
    }

    public RecruitmentMatchScoreResponse getMatchScore(UUID userId, UUID jobId) {
        RecruitmentMatchScoreResponse response;
        try {
            response = restClient.get()
                    .uri(MATCH_SCORE_PATH, userId, jobId)
                    .header("X-Internal-Service-Token", internalServiceToken)
                    .retrieve()
                    .body(RecruitmentMatchScoreResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Match score not found", "NOT_FOUND");
            }
            throw upstreamFailure();
        } catch (RestClientException exception) {
            throw upstreamFailure();
        }

        if (response == null || !userId.equals(response.userId()) || !jobId.equals(response.jobId())) {
            throw upstreamFailure();
        }
        return response;
    }

    private ApiException upstreamFailure() {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "AI Career service returned an invalid or unavailable response",
                "UPSTREAM_ERROR");
    }
}
