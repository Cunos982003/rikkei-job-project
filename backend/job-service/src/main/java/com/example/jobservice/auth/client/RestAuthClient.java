package com.example.jobservice.auth.client;

import com.example.jobservice.common.exception.BadGatewayException;
import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "auth.client.stub", havingValue = "false", matchIfMissing = true)
public class RestAuthClient implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(RestAuthClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestAuthClient(
            @Value("${auth.base-url:http://localhost:8081}") String baseUrl,
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

    private RestClient.RequestHeadersSpec<?> withAuth(RestClient.RequestHeadersSpec<?> spec) {
        String authHeader = getAuthorizationHeader();
        if (authHeader != null) {
            return spec.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return spec;
    }

    private <T> T execute(RestClient.RequestHeadersSpec<?> spec, ParameterizedTypeReference<AuthEnvelope<T>> typeRef, boolean isCreateHr) {
        try {
            AuthEnvelope<T> response = withAuth(spec)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        int statusCode = res.getStatusCode().value();
                        String body = "";
                        try {
                            body = new String(res.getBody().readAllBytes());
                        } catch (Exception ignored) {
                        }

                        String message = extractErrorMessage(body);

                        if (statusCode == 409) {
                            if (isCreateHr) {
                                throw new ConflictException("Email đã tồn tại");
                            } else {
                                throw new ConflictException(message != null ? message : "Xung đột dữ liệu");
                            }
                        } else if (statusCode == 404) {
                            throw new ResourceNotFoundException(
                                    message != null ? message : "Không tìm thấy tài nguyên tại auth-service");
                        } else if (statusCode >= 400 && statusCode < 500) {
                            throw new BadRequestException(
                                    message != null ? message : "Yêu cầu không hợp lệ");
                        } else {
                            throw new BadGatewayException("Dịch vụ xác thực tạm thời không khả dụng");
                        }
                    })
                    .body(typeRef);

            if (response == null || response.data() == null) {
                return null;
            }
            return response.data();
        } catch (ConflictException | ResourceNotFoundException | BadRequestException | BadGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with auth-service: {}", e.getMessage(), e);
            throw new BadGatewayException("Dịch vụ xác thực tạm thời không khả dụng");
        }
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("message") && !node.get("message").isNull()) {
                return node.get("message").asText();
            }
            if (node.has("detail") && !node.get("detail").isNull()) {
                return node.get("detail").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public AuthHrAccount createHrAccount(String email, String fullName, UUID companyId) {
        Map<String, Object> requestBody = Map.of(
                "email", email,
                "fullName", fullName,
                "companyId", companyId
        );

        ParameterizedTypeReference<AuthEnvelope<AuthHrAccount>> typeRef = new ParameterizedTypeReference<>() {};
        return execute(
                restClient.post()
                        .uri("/api/v1/auth/internal/hr-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody),
                typeRef,
                true
        );
    }

    @Override
    public AuthHrAccount lockHrAccount(UUID userId, String reason) {
        Map<String, Object> requestBody = Map.of("reason", reason);

        ParameterizedTypeReference<AuthEnvelope<AuthHrAccount>> typeRef = new ParameterizedTypeReference<>() {};
        return execute(
                restClient.post()
                        .uri("/api/v1/auth/internal/hr-accounts/{id}/lock", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody),
                typeRef,
                false
        );
    }

    @Override
    public AuthHrAccount unlockHrAccount(UUID userId) {
        ParameterizedTypeReference<AuthEnvelope<AuthHrAccount>> typeRef = new ParameterizedTypeReference<>() {};
        return execute(
                restClient.post()
                        .uri("/api/v1/auth/internal/hr-accounts/{id}/unlock", userId),
                typeRef,
                false
        );
    }

    @Override
    public List<AuthHrAccount> listHrAccounts(UUID companyId) {
        ParameterizedTypeReference<AuthEnvelope<List<AuthHrAccount>>> typeRef = new ParameterizedTypeReference<>() {};
        List<AuthHrAccount> result = execute(
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/auth/internal/hr-accounts")
                                .queryParam("companyId", companyId)
                                .build()),
                typeRef,
                false
        );
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public Map<UUID, Long> countHrAccounts(Collection<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String idsJoined = companyIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        ParameterizedTypeReference<AuthEnvelope<Map<String, Long>>> typeRef = new ParameterizedTypeReference<>() {};
        Map<String, Long> rawMap = execute(
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/auth/internal/hr-accounts/count")
                                .queryParam("companyIds", idsJoined)
                                .build()),
                typeRef,
                false
        );

        Map<UUID, Long> result = new HashMap<>();
        if (rawMap != null) {
            for (Map.Entry<String, Long> entry : rawMap.entrySet()) {
                try {
                    result.put(UUID.fromString(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }
}
