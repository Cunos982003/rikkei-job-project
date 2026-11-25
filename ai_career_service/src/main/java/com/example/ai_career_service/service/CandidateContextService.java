package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.ApiResponse;
import com.example.ai_career_service.dto.ProfileSkillResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Fetches a non-PII candidate competency summary (no contact info such as email, phone, address, or social links)
 * to ground career assistant responses.
 */
@Service
@Slf4j
public class CandidateContextService {

    private final RestClient profileClient;

    public CandidateContextService(
            @Value("${app.integrations.profile-service-url:http://localhost:8082}") String profileServiceUrl) {
        this.profileClient = RestClient.builder().baseUrl(profileServiceUrl).build();
    }

    @SuppressWarnings("unchecked")
    public String buildProfileSummary(UUID userId) {
        StringBuilder summary = new StringBuilder();

        try {
            Map<String, Object> profileRes = profileClient.get()
                    .uri("/api/v1/profiles/me")
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (profileRes != null && profileRes.get("data") instanceof Map<?, ?> data) {
                appendIfPresent(summary, "Chức danh / Mục tiêu", data.get("headline"));
                appendIfPresent(summary, "Định hướng nghề nghiệp", data.get("careerGoal"));
                appendIfPresent(summary, "Giới thiệu năng lực", data.get("bio"));
                appendIfPresent(summary, "Tóm tắt", data.get("summary"));
                appendIfPresent(summary, "Trường học", data.get("university"));
                appendIfPresent(summary, "Chuyên ngành", data.get("major"));
                appendIfPresent(summary, "Số năm kinh nghiệm", data.get("yearsOfExperience"));
                appendIfPresent(summary, "Cấp độ hiện tại", data.get("level"));
            }
        } catch (Exception ex) {
            log.debug("Profile details unavailable for user {}: {}", userId, ex.getMessage());
        }

        try {
            ApiResponse<List<ProfileSkillResponse>> skillsRes = profileClient.get()
                    .uri("/api/v1/profiles/me/skills")
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<ProfileSkillResponse>>>() {});

            if (skillsRes != null && skillsRes.success() && skillsRes.data() != null && !skillsRes.data().isEmpty()) {
                summary.append("- Kỹ năng năng lực: ");
                List<String> skillEntries = skillsRes.data().stream()
                        .map(s -> {
                            String name = (s.skillName() != null && !s.skillName().isBlank()) ? s.skillName() : s.skillCode();
                            if (name == null || name.isBlank()) {
                                return null;
                            }
                            StringBuilder sb = new StringBuilder(name);
                            if (s.verifiedScore() != null) {
                                sb.append(" (verified: ").append(s.verifiedScore()).append("/100)");
                            } else if (s.selfScore() != null) {
                                sb.append(" (tự đánh giá: ").append(s.selfScore()).append("/100)");
                            }
                            if (s.level() != null && !s.level().isBlank()) {
                                sb.append(" [").append(s.level()).append("]");
                            }
                            return sb.toString();
                        })
                        .filter(entry -> entry != null)
                        .toList();
                summary.append(String.join("; ", skillEntries)).append("\n");
            }
        } catch (Exception ex) {
            log.debug("Profile skills unavailable for user {}: {}", userId, ex.getMessage());
        }

        if (summary.isEmpty()) {
            return "Chưa có dữ liệu hồ sơ năng lực chi tiết (ứng viên mới hoặc đang cập nhật kỹ năng).";
        }
        return summary.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
            sb.append("- ").append(label).append(": ").append(text).append("\n");
        }
    }
}
