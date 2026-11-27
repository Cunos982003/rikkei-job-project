package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.common.api.PageData;
import com.example.profileservice.common.domain.Level;
import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillScore;
import com.example.profileservice.repository.CandidateProfileRepository;
import com.example.profileservice.repository.SkillRepository;
import com.example.profileservice.repository.SkillScoreRepository;
import com.example.profileservice.service.ContactAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CandidateDirectoryController {

    private final CandidateProfileRepository profileRepository;
    private final SkillScoreRepository skillScoreRepository;
    private final SkillRepository skillRepository;
    private final ContactAccessService contactAccessService;

    @GetMapping("/candidate-directory/skills")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listSkills() {
        List<Map<String, Object>> result = skillRepository.findAll().stream()
                .sorted(Comparator.comparing(Skill::getName, String.CASE_INSENSITIVE_ORDER))
                .map(skill -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", skill.getId());
                    item.put("code", skill.getCode());
                    item.put("name", skill.getName());
                    item.put("group", skill.getGroup());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỹ năng thành công", result));
    }

    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<PageData<Map<String, Object>>>> listCandidates(
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Company-Id", required = false) UUID companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        Map<UUID, Skill> skillById = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity(), (a, b) -> a));

        List<CandidateProfile> allProfiles = profileRepository.findAll().stream()
                .filter(p -> p.getVisibility() == Visibility.PUBLIC
                        || p.getVisibility() == Visibility.RECRUITER)
                .filter(p -> matchesKeyword(p, keyword))
                .sorted(Comparator.comparing(
                        CandidateProfile::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = allProfiles.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        List<Map<String, Object>> items = new ArrayList<>();
        for (CandidateProfile profile : allProfiles.subList(fromIndex, toIndex)) {
            boolean canSee = canSeeContactForProfile(requesterId, role, companyId, profile);
            items.add(toCandidatePayload(profile, skillById, canSee));
        }

        Page<Map<String, Object>> pageResult = new PageImpl<>(items, PageRequest.of(safePage, safeSize), total);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ứng viên thành công", PageData.from(pageResult)));
    }

    @GetMapping("/candidates/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCandidateById(
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Company-Id", required = false) UUID companyId,
            @PathVariable UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy ứng viên"));

        Map<UUID, Skill> skillById = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity(), (a, b) -> a));

        boolean canSee = canSeeContactForProfile(requesterId, role, companyId, profile);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin ứng viên thành công", toCandidatePayload(profile, skillById, canSee)));
    }

    private boolean canSeeContactForProfile(UUID requesterId, String role, UUID companyId, CandidateProfile profile) {
        if (requesterId != null && requesterId.equals(profile.getUserId())) {
            return true;
        }
        if ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return true;
        }
        UUID effectiveCompanyId = companyId;
        if (effectiveCompanyId == null && "HR".equalsIgnoreCase(role)) {
            effectiveCompanyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        if (effectiveCompanyId != null && contactAccessService != null) {
            return contactAccessService.canSeeContact(effectiveCompanyId, profile.getUserId());
        }
        return false;
    }

    private boolean matchesKeyword(CandidateProfile p, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        return contains(p.getFullName(), kw)
                || contains(p.getEmail(), kw)
                || contains(p.getHeadline(), kw)
                || contains(p.getProgram(), kw)
                || contains(p.getCourseName(), kw);
    }

    private boolean contains(String field, String kw) {
        return field != null && field.toLowerCase().contains(kw);
    }

    private Map<String, Object> toCandidatePayload(CandidateProfile profile, Map<UUID, Skill> skillById, boolean canSeeContact) {
        List<SkillScore> scores = skillScoreRepository.findByUserId(profile.getUserId());
        List<Map<String, Object>> skills = new ArrayList<>(scores.size());
        double totalScore = 0;
        int scoredCount = 0;

        for (SkillScore score : scores) {
            Skill skill = skillById.get(score.getSkillId());
            if (skill == null)
                continue;
            Short effectiveScore = score.getVerifiedScore() != null ? score.getVerifiedScore() : score.getSelfScore();
            int numericScore = effectiveScore != null ? effectiveScore.intValue() : 0;
            if (effectiveScore != null) {
                totalScore += numericScore;
                scoredCount++;
            }
            Map<String, Object> skillItem = new LinkedHashMap<>();
            skillItem.put("name", skill.getName());
            skillItem.put("score", numericScore);
            skillItem.put("level", score.getLevel());
            skillItem.put("verified", score.getVerifiedScore() != null);
            skills.add(skillItem);
        }

        int overallScore = scoredCount > 0 ? (int) Math.round(totalScore / scoredCount) : 0;

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", profile.getUserId());
        item.put("fullName", profile.getFullName());
        if (canSeeContact) {
            item.put("email", profile.getEmail());
            item.put("phone", profile.getPhone());
            item.put("contactLocked", false);
        } else {
            item.put("email", null);
            item.put("phone", null);
            item.put("contactLocked", true);
        }
        item.put("avatarUrl", profile.getAvatarUrl());
        item.put("title", profile.getHeadline() != null ? profile.getHeadline() : profile.getProgram());
        item.put("headline", profile.getHeadline());
        item.put("program", profile.getProgram());
        item.put("overallScore", overallScore);
        item.put("level", resolveLevel(overallScore));
        item.put("skills", skills);
        item.put("isPublic", profile.getVisibility() == Visibility.PUBLIC);
        item.put("createdAt", profile.getCreatedAt());
        return item;
    }

    private String resolveLevel(int score) {
        return Level.fromScore((short) score).name();
    }
}
