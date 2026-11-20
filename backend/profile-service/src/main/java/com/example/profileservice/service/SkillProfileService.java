package com.example.profileservice.service;

import com.example.profileservice.common.domain.Score;
import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.dto.SelfSkillDto;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillEvidence;
import com.example.profileservice.entity.SkillScore;
import com.example.profileservice.repository.SkillEvidenceRepository;
import com.example.profileservice.repository.SkillRepository;
import com.example.profileservice.repository.SkillScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for self-skills management.
 * Extracted from ProfileService as part of service decomposition (Task 4.2).
 */
@Service
@RequiredArgsConstructor
public class SkillProfileService {

    private final SkillEvidenceRepository evidenceRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public void upsertSelfSkills(UUID userId, List<SelfSkillDto> skills) {
        upsertSelfSkills(UserId.of(userId), skills);
    }

    @Transactional
    public void upsertSelfSkills(UserId userId, List<SelfSkillDto> skills) {
        // Handle null skills list
        if (skills == null || skills.isEmpty()) {
            return;
        }

        for (SelfSkillDto dto : skills) {
            // Handle null skillId
            if (dto.getSkillId() == null) {
                throw new IllegalArgumentException("skillId must not be null");
            }

            if (dto.getSelfScore() == null || dto.getSelfScore() < 0 || dto.getSelfScore() > 100) {
                throw new IllegalArgumentException(
                        "selfScore must be between 0 and 100 for skill: " + dto.getSkillId());
            }

            Skill skill = skillRepo.findById(dto.getSkillId())
                    .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + dto.getSkillId()));

            // Upsert evidence with source=SELF
            SkillEvidence evidence = evidenceRepo.findByUserIdAndSkillIdAndSourceAndSourceRef(
                    userId.value(), skill.getId(), "SELF", "SELF_" + skill.getCode())
                    .orElseGet(() -> SkillEvidence.builder()
                            .userId(userId.value())
                            .skill(skill)
                            .source("SELF")
                            .sourceRef("SELF_" + skill.getCode())
                            .title("Self-reported " + skill.getName())
                            .verified(false)
                            .build());

            // Use domain method to update score and recordedAt
            evidence.updateScore(Score.of(dto.getSelfScore().shortValue()));
            evidenceRepo.save(evidence);

            // Upsert skill_score and use domain recalculate
            SkillScore score = scoreRepo.findByUserIdAndSkillId(userId.value(), skill.getId())
                    .orElseGet(() -> SkillScore.builder()
                            .userId(userId.value())
                            .skillId(skill.getId())
                            .build());

            // Get all evidences for this user and skill to recalculate
            List<SkillEvidence> allEvidences = evidenceRepo.findByUserIdAndSkillId(userId.value(), skill.getId());
            score.recalculate(allEvidences);
            scoreRepo.save(score);
        }
    }

    /**
     * Upsert self-skills from JSON string (for staff edit self_skills).
     * JSON format: [{"skillId": "uuid", "selfScore": 80}, ...]
     */
    @Transactional
    public void upsertSelfSkillsFromJson(UUID userId, String json) {
        upsertSelfSkillsFromJson(UserId.of(userId), json);
    }

    @Transactional
    public void upsertSelfSkillsFromJson(UserId userId, String json) {
        try {
            List<SelfSkillDto> skills = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SelfSkillDto.class));
            upsertSelfSkills(userId, skills);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid self_skills JSON: " + e.getMessage(), e);
        }
    }
}