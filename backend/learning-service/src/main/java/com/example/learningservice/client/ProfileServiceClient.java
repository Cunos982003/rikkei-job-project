package com.example.learningservice.client;

import com.example.learningservice.model.dto.response.ProfileSkillResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final ObjectMapper objectMapper;

    public ProfileServiceClient(
            RestClient.Builder restClientBuilder,
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${app.integrations.profile-service-url:http://localhost:8082}") String profileServiceUrl) {
        this.profileServiceUrl = profileServiceUrl;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.restClient = restClientBuilder
                .baseUrl(profileServiceUrl)
                .build();
    }

    public List<ProfileSkillResponse> getCandidateSkills(UUID userId, String authorizationHeader) {
        log.debug("Gọi dịch vụ hồ sơ tại {}/api/v1/profiles/me/skills cho userId: {}", profileServiceUrl, userId);
        try {
            String rawJson = restClient.get()
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
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("Dịch vụ hồ sơ trả về phản hồi rỗng cho userId: {}", userId);
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            // Bóc tách linh hoạt: nếu bọc trong ApiResponse thì lấy trường "data", nếu là mảng thô thì lấy root
            JsonNode dataNode = (root.has("data") && !root.get("data").isNull()) ? root.get("data") : root;

            if (dataNode != null && dataNode.isArray()) {
                List<ProfileSkillResponse> skills = objectMapper.convertValue(
                        dataNode,
                        new TypeReference<List<ProfileSkillResponse>>() {}
                );
                log.info("Lấy thành công {} kỹ năng từ dịch vụ hồ sơ cho userId: {}", skills != null ? skills.size() : 0, userId);
                return skills != null ? skills : Collections.emptyList();
            }

            log.warn("Dữ liệu kỹ năng từ dịch vụ hồ sơ không phải là mảng hợp lệ cho userId: {}", userId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Không thể kết nối hoặc phân tích dữ liệu từ dịch vụ hồ sơ tại {} cho userId {}: {}", profileServiceUrl, userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean syncLmsSkillEvidence(UUID userId, String skillCode, String courseId, String courseName, Short score) {
        log.info("Đồng bộ bằng chứng LMS sang profile-service: userId={}, skillCode={}, courseId={}, score={}",
                userId, skillCode, courseId, score);
        try {
            restClient.post()
                    .uri("/api/v1/profiles/internal/lms-evidence")
                    .body(java.util.Map.of(
                            "userId", userId.toString(),
                            "skillCode", skillCode,
                            "courseId", courseId,
                            "courseName", courseName != null ? courseName : courseId,
                            "score", score
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Đồng bộ bằng chứng LMS sang profile-service thành công");
            return true;
        } catch (Exception e) {
            log.warn("Không thể đồng bộ bằng chứng LMS sang profile-service tại {}: {}", profileServiceUrl, e.getMessage());
            return false;
        }
    }
}
