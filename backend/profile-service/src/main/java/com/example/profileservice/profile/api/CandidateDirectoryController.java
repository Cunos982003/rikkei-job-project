package com.example.profileservice.profile.api;

import com.example.profileservice.common.domain.Level;
import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/candidates")
    public ResponseEntity<Map<String, Object>> listCandidates(
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
            items.add(toCandidatePayload(profile, skillById));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("total", total);
        response.put("page", safePage);
        response.put("size", safeSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/candidates/{userId}")
    public ResponseEntity<Map<String, Object>> getCandidateById(@PathVariable UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));

        Map<UUID, Skill> skillById = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity(), (a, b) -> a));

        return ResponseEntity.ok(toCandidatePayload(profile, skillById));
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

    private Map<String, Object> toCandidatePayload(CandidateProfile profile, Map<UUID, Skill> skillById) {
        List<SkillScore> scores = skillScoreRepository.findByUserId(profile.getUserId());
        List<Map<String, Object>> skills = new ArrayList<>(scores.size());
        double totalScore = 0;
        int scoredCount = 0;

        for (SkillScore score : scores) {
            Skill skill = skillById.get(score.getSkillId());
            if (skill == null) continue;
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
        item.put("email", profile.getEmail());
        item.put("phone", profile.getPhone());
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
