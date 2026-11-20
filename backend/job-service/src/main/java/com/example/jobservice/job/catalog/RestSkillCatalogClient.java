package com.example.jobservice.job.catalog;

import com.example.jobservice.common.exception.BadGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "skill.catalog.stub", havingValue = "false", matchIfMissing = true)
public class RestSkillCatalogClient implements SkillCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(RestSkillCatalogClient.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private volatile List<CatalogSkill> cachedSkills = null;
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    public RestSkillCatalogClient(
            @Value("${skill.catalog.base-url:http://localhost:8082}") String baseUrl,
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
    public List<CatalogSkill> listSkills() {
        Instant now = Instant.now();
        List<CatalogSkill> cached = this.cachedSkills;
        if (cached != null && now.isBefore(cacheExpiresAt)) {
            return cached;
        }

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri("/api/v1/skills")
                    .accept(MediaType.APPLICATION_JSON);

            String authHeader = getAuthorizationHeader();
            if (authHeader != null) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            String responseBody = spec.retrieve().body(String.class);
            List<CatalogSkill> parsed = parseSkills(responseBody);

            this.cachedSkills = parsed;
            this.cacheExpiresAt = now.plus(CACHE_TTL);
            return parsed;
        } catch (Exception ex) {
            log.error("Failed to fetch skills from profile-service: {}", ex.getMessage(), ex);
            throw new BadGatewayException("Dịch vụ danh mục kỹ năng tạm thời không khả dụng");
        }
    }

    private List<CatalogSkill> parseSkills(String responseBody) {
        List<CatalogSkill> list = new ArrayList<>();
        if (responseBody == null || responseBody.isBlank()) {
            return list;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root;
            if (root.isObject() && root.has("data")) {
                dataNode = root.get("data");
            }

            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    String id = item.hasNonNull("id") ? item.get("id").asText() : "";
                    String code = item.hasNonNull("code") ? item.get("code").asText() : "";
                    String name = item.hasNonNull("name") ? item.get("name").asText() : "";
                    String group = item.hasNonNull("group") ? item.get("group").asText() : "";
                    list.add(new CatalogSkill(id, code, name, group));
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse skills response: {}", ex.getMessage());
            throw new BadGatewayException("Dịch vụ danh mục kỹ năng tạm thời không khả dụng");
        }
        return list;
    }

    @Override
    public Optional<CatalogSkill> findByIdOrCode(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return Optional.empty();
        }
        String target = idOrCode.trim();
        return listSkills().stream()
                .filter(s -> s.id().equalsIgnoreCase(target) || s.code().equalsIgnoreCase(target))
                .findFirst();
    }
}

