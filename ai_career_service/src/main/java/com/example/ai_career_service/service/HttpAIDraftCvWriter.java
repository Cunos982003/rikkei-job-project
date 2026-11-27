package com.example.ai_career_service.service;

import com.example.ai_career_service.model.AIDraft;
import com.example.common.logging.TracingClientHttpRequestInterceptor;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Primary
public class HttpAIDraftCvWriter implements AIDraftCvWriter {

    private final RestClient profileClient;

    public HttpAIDraftCvWriter(
            @Value("${app.integrations.profile-service-url:http://localhost:8082}") String profileServiceUrl) {
        this.profileClient = RestClient.builder()
                .requestInterceptor(new TracingClientHttpRequestInterceptor())
                .baseUrl(profileServiceUrl)
                .build();
    }

    @Override
    public void writeDraft(AIDraft draft) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", draft.getUserId());
        body.put("draftId", draft.getId());
        body.put("section", draft.getSection().name());
        body.put("projectId", draft.getProjectId());
        body.put("content", draft.getOutput());

        try {
            profileClient.post()
                    .uri("/api/v1/profiles/internal/cvs/{cvId}/apply-draft", draft.getCvId().toString())
                    .header("X-User-Id", draft.getUserId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == HttpStatus.FORBIDDEN || status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(status, ex.getResponseBodyAsString(), ex);
            }
            log.warn("Failed to apply AI draft {} to CV {}: status={} body={}",
                    draft.getId(), draft.getCvId(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể ghi bản nháp vào CV trên profile-service",
                    ex);
        } catch (RestClientException ex) {
            log.warn("Profile service unavailable while applying draft {}: {}", draft.getId(), ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile service tạm thời không khả dụng",
                    ex);
        }
    }
}
