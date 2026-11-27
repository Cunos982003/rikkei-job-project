package com.example.ai_career_service.service;

import com.example.ai_career_service.enums.AIDraftSection;
import com.example.common.logging.TracingClientHttpRequestInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Primary
public class HttpAIDraftDataSource implements AIDraftDataSource {

    private final RestClient profileClient;
    private final RestClient jobClient;
    private final ObjectMapper objectMapper;

    public HttpAIDraftDataSource(
            @Value("${app.integrations.profile-service-url:http://localhost:8082}") String profileServiceUrl,
            @Value("${app.integrations.job-service-url:http://localhost:8085}") String jobServiceUrl,
            ObjectMapper objectMapper) {
        this.profileClient = RestClient.builder()
                .requestInterceptor(new TracingClientHttpRequestInterceptor())
                .baseUrl(profileServiceUrl)
                .build();
        this.jobClient = RestClient.builder()
                .requestInterceptor(new TracingClientHttpRequestInterceptor())
                .baseUrl(jobServiceUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode loadInputSnapshot(
            UUID userId,
            UUID cvId,
            AIDraftSection section,
            UUID projectId,
            UUID jobId) {
        ObjectNode snapshot = fetchProfileSnapshot(userId, cvId, projectId);
        snapshot.put("requestedSection", section.name());
        if (jobId != null) {
            snapshot.set("jobSkills", fetchJobSkills(jobId));
        }
        return snapshot;
    }

    private ObjectNode fetchProfileSnapshot(UUID userId, UUID cvId, UUID projectId) {
        try {
            JsonNode response = profileClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v1/profiles/internal/cvs/{cvId}/snapshot")
                                .queryParam("userId", userId.toString());
                        if (projectId != null) {
                            builder.queryParam("projectId", projectId.toString());
                        }
                        return builder.build(cvId.toString());
                    })
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("data") || !response.get("data").isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Profile service returned an invalid CV snapshot");
            }
            return (ObjectNode) response.get("data").deepCopy();
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == HttpStatus.FORBIDDEN || status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(status, ex.getResponseBodyAsString(), ex);
            }
            log.warn("Profile service snapshot failed for cvId={} userId={}: status={} body={}",
                    cvId, userId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tải dữ liệu hồ sơ từ profile-service",
                    ex);
        } catch (RestClientException ex) {
            log.warn("Profile service unavailable for cvId={} userId={}: {}", cvId, userId, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile service tạm thời không khả dụng",
                    ex);
        }
    }

    private ArrayNode fetchJobSkills(UUID jobId) {
        try {
            JsonNode response = jobClient.get()
                    .uri("/api/v1/jobs/{jobId}", jobId.toString())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("data") || !response.get("data").isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Job service returned an invalid job payload");
            }
            JsonNode data = response.get("data");
            ArrayNode jobSkillsNode = objectMapper.createArrayNode();
            JsonNode rawSkills = data.get("jobSkills");
            if (rawSkills != null && rawSkills.isArray()) {
                for (JsonNode skill : rawSkills) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    if (skill.hasNonNull("skillId")) {
                        item.put("skillId", skill.get("skillId").asText());
                    }
                    if (skill.hasNonNull("skillName")) {
                        item.put("skillName", skill.get("skillName").asText());
                    }
                    if (skill.hasNonNull("required")) {
                        item.put("required", skill.get("required").asBoolean());
                    }
                    jobSkillsNode.add(objectMapper.valueToTree(item));
                }
            }
            return jobSkillsNode;
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng mục tiêu", ex);
            }
            log.warn("Job service failed for jobId={}: status={}", jobId, ex.getStatusCode());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tải kỹ năng từ tin tuyển dụng mục tiêu",
                    ex);
        } catch (RestClientException ex) {
            log.warn("Job service unavailable for jobId={}: {}", jobId, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Job service tạm thời không khả dụng",
                    ex);
        }
    }
}
