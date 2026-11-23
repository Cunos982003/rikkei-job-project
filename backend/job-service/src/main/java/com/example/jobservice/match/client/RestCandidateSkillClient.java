package com.example.jobservice.match.client;

import com.example.jobservice.common.exception.BadGatewayException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "candidate.skills.stub", havingValue = "false", matchIfMissing = true)
public class RestCandidateSkillClient implements CandidateSkillClient {

    private static final Logger log = LoggerFactory.getLogger(RestCandidateSkillClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestCandidateSkillClient(
            @Value("${candidate.skills.base-url:${PROFILE_SERVICE_URL:http://localhost:8082}}") String baseUrl,
            @Autowired(required = false) ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    private String getAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && !header.isBlank()) {
                return header;
            }
        }
        return null;
    }

    @Override
    public List<CandidateSkillScore> getSkills(UUID candidateUserId) {
        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri("/api/v1/profiles/internal/{userId}/skill-scores", candidateUserId)
                    .accept(MediaType.APPLICATION_JSON);

            String authHeader = getAuthorizationHeader();
            if (authHeader != null) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            String responseBody = spec.retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        int statusCode = res.getStatusCode().value();
                        if (statusCode == 404) {
                            throw new ResourceNotFoundException("Không tìm thấy hồ sơ ứng viên");
                        }
                        throw new BadGatewayException("Dịch vụ hồ sơ ứng viên tạm thời không khả dụng");
                    })
                    .body(String.class);

            return parseCandidateSkills(responseBody);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Failed to fetch candidate skills from profile-service for userId={}: {}", candidateUserId, ex.getMessage(), ex);
            throw new BadGatewayException("Dịch vụ hồ sơ ứng viên tạm thời không khả dụng");
        }
    }

    private List<CandidateSkillScore> parseCandidateSkills(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode targetArrayNode = root;
            if (root.isObject() && root.has("data")) {
                targetArrayNode = root.get("data");
            }
            if (targetArrayNode == null || !targetArrayNode.isArray()) {
                return List.of();
            }
            List<CandidateSkillScore> list = new ArrayList<>();
            for (JsonNode item : targetArrayNode) {
                String skillId = item.hasNonNull("skillId") ? item.get("skillId").asText() : null;
                String skillCode = item.hasNonNull("skillCode") ? item.get("skillCode").asText() : null;
                String skillName = item.hasNonNull("skillName") ? item.get("skillName").asText() : null;
                Integer verifiedScore = item.hasNonNull("verifiedScore") ? item.get("verifiedScore").asInt() : null;
                Integer selfScore = item.hasNonNull("selfScore") ? item.get("selfScore").asInt() : null;
                list.add(new CandidateSkillScore(skillId, skillCode, skillName, verifiedScore, selfScore));
            }
            return list;
        } catch (Exception e) {
            log.error("Failed to parse candidate skill response: {}", e.getMessage(), e);
            throw new BadGatewayException("Dịch vụ hồ sơ ứng viên tạm thời không khả dụng");
        }
    }
}

