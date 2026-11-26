package com.example.ai_career_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.example.ai_career_service.dto.ApiResponse;
import com.example.ai_career_service.dto.ProfileSkillResponse;

@Component
@Primary
@Profile("!test")
@Slf4j
public class HttpRecommendationDataSource implements RecommendationDataSource {

    private final RestClient profileClient;
    private final RestClient jobClient;

    public HttpRecommendationDataSource(
            @Value("${app.integrations.profile-service-url:http://localhost:8082}") String profileServiceUrl,
            @Value("${app.integrations.job-service-url:http://localhost:8085}") String jobServiceUrl) {
        this.profileClient = RestClient.builder().baseUrl(profileServiceUrl).build();
        this.jobClient = RestClient.builder().baseUrl(jobServiceUrl).build();
    }

    @Override
    public RecommendationSnapshot loadSnapshot(UUID userId) {
        List<RecommendationSnapshot.VerifiedSkill> verifiedSkills = fetchVerifiedSkills(userId);
        List<RecommendationSnapshot.EligibleJob> eligibleJobs = fetchEligibleJobs();
        String profileRevision = "profile-" + userId + "-" + verifiedSkills.hashCode();
        return new RecommendationSnapshot(profileRevision, verifiedSkills, eligibleJobs);
    }


private List<RecommendationSnapshot.VerifiedSkill> fetchVerifiedSkills(UUID userId) {
    try {
        ApiResponse<List<ProfileSkillResponse>> response = profileClient.get()
                .uri("/api/v1/profiles/me/skills")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<
                        ApiResponse<List<ProfileSkillResponse>>
                >() {});

        if (response == null || !response.success() || response.data() == null) {
            return List.of();
        }

        List<RecommendationSnapshot.VerifiedSkill> result = new ArrayList<>();

        for (ProfileSkillResponse skill : response.data()) {
            if (skill.verifiedScore() == null) {
                continue;
            }

            String name = skill.skillName();

            if (name == null || name.isBlank()) {
                name = skill.skillCode();
            }

            if (name == null || name.isBlank()) {
                continue;
            }

            int index = clampScore(skill.verifiedScore().intValue());

            result.add(new RecommendationSnapshot.VerifiedSkill(name, index));
        }

        return result;

    } catch (Exception ex) {
        log.error(
                "Failed to fetch candidate skills from profile-service for user {}",
                userId,
                ex
        );

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to load candidate skills from profile-service",
                ex
        );
    }
}

    @SuppressWarnings("unchecked")
    private List<RecommendationSnapshot.EligibleJob> fetchEligibleJobs() {
        try {
            Map<String, Object> response = jobClient.get()
                    .uri("/jobs?status=APPROVED&size=200")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            // job-service wraps response as ApiResponse<PageResult>:
            // { "success": true, "data": { "items": [...], "total": N } }
            List<?> rawItems = null;
            if (response != null) {
                if (response.get("items") instanceof List<?> rootItems) {
                    // direct list at root (mock or legacy)
                    rawItems = rootItems;
                } else if (response.get("data") instanceof Map<?, ?> dataMap
                        && dataMap.get("items") instanceof List<?> nestedItems) {
                    // wrapped ApiResponse<PageResult> format
                    rawItems = nestedItems;
                }
            }
            if (rawItems == null) {
                return List.of();
            }
            List<RecommendationSnapshot.EligibleJob> jobs = new ArrayList<>();
            for (Object itemObj : rawItems) {
                if (!(itemObj instanceof Map<?, ?> item)) {
                    continue;
                }
                String idStr = stringOrNull(item.get("id"));
                String title = stringOrNull(item.get("title"));
                String companyName = stringOrNull(item.get("companyName"));
                if (idStr == null || isExpired(item.get("deadline"))) {
                    continue;
                }
                UUID jobId;
                try {
                    jobId = UUID.fromString(idStr);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                String revision = "job-" + idStr + "-" + stringOrDefault(item.get("createdAt"), "v1");
                List<RecommendationSnapshot.SkillRequirement> requirements = new ArrayList<>();
                if (item.get("jobSkills") instanceof List<?> rawJobSkills) {
                    for (Object jsObj : rawJobSkills) {
                        if (!(jsObj instanceof Map<?, ?> js)) {
                            continue;
                        }
                        String skillName = stringOrNull(js.get("skillName"));
                        if (skillName == null || skillName.isBlank()) {
                            skillName = stringOrNull(js.get("skillId"));
                        }
                        if (skillName == null || skillName.isBlank()) {
                            continue;
                        }
                        int minScore = js.get("minScore") instanceof Number n ? clampScore(n.intValue()) : 60;
                        boolean mandatory = !(js.get("isMandatory") instanceof Boolean b) || b;
                        requirements.add(new RecommendationSnapshot.SkillRequirement(
                                skillName,
                                minScore,
                                mandatory
                                        ? RecommendationSnapshot.RequirementType.REQUIRED
                                        : RecommendationSnapshot.RequirementType.PREFERRED));
                    }
                }
                jobs.add(new RecommendationSnapshot.EligibleJob(jobId, title, companyName, revision, requirements));
            }
            return jobs;
        } catch (Exception ex) {
            log.error("Failed to fetch approved jobs from job-service", ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to load approved jobs from job-service",
                    ex);
        }
    }

    private static boolean isExpired(Object deadlineObj) {
        if (deadlineObj == null) {
            return false;
        }
        String raw = deadlineObj.toString().trim();
        if (raw.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        try {
            if (raw.length() >= 10) {
                return LocalDate.parse(raw.substring(0, 10)).isBefore(today);
            }
            return Instant.parse(raw).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().isBefore(today);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

    private static String stringOrDefault(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }
}
