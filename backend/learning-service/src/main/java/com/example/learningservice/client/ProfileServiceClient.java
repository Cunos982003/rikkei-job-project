package com.example.learningservice.client;

import com.example.learningservice.model.dto.response.ProfileSkillResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ProfileServiceClient {

    private final RestClient restClient;
    private final String profileServiceUrl;

    public ProfileServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.integrations.profile-service-url:http://localhost:8082}") String profileServiceUrl) {
        this.profileServiceUrl = profileServiceUrl;
        this.restClient = restClientBuilder
                .baseUrl(profileServiceUrl)
                .build();
    }

    public List<ProfileSkillResponse> getCandidateSkills(UUID userId, String authorizationHeader) {
        log.debug("Gọi dịch vụ hồ sơ tại {}/api/v1/profiles/me/skills cho userId: {}", profileServiceUrl, userId);
        try {
            List<ProfileSkillResponse> skills = restClient.get()
                    .uri("/api/v1/profiles/me/skills")
                    .header("X-User-Id", userId.toString())
                    .headers(headers -> {
                        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Dịch vụ hồ sơ trả về trạng thái lỗi {} cho userId: {}", resp.getStatusCode(), userId);
                    })
                    .body(new ParameterizedTypeReference<List<ProfileSkillResponse>>() {});

            return skills != null ? skills : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Không thể kết nối đến dịch vụ hồ sơ tại {} cho userId {}: {}", profileServiceUrl, userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

