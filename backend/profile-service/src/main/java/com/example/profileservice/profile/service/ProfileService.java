package com.example.profileservice.profile.service;

import com.example.profileservice.common.LevelConstants;
import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.LmsClient;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository profileRepo;
    private final LmsClient lmsClient;
    private final SkillEvidenceRepository evidenceRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;

    @Transactional
    public CandidateProfile getOrCreateProfile(UUID userId) {
        return profileRepo.findByUserId(userId)
                .orElseGet(() -> createProfileFromLms(userId));
    }

    private CandidateProfile createProfileFromLms(UUID userId) {
        LmsStudentDto lmsStudent = lmsClient.getStudent(userId).block();
        if (lmsStudent == null) {
            throw new IllegalStateException("LMS returned null student for userId: " + userId);
        }

        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .fullName(lmsStudent.getFullName())
                .email(lmsStudent.getEmail())
                .program(lmsStudent.getProgram())
                .classCode(lmsStudent.getClassCode())
                .courseName(lmsStudent.getCourseName())
                .build();

        return profileRepo.save(profile);
    }

    @Transactional
    public CandidateProfile updateProfile(UUID userId, CandidateProfile update) {
        CandidateProfile existing = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));

        // LMS-sourced fields are immutable - do not update
        existing.setFullName(update.getFullName());
        existing.setDob(update.getDob());
        existing.setGender(update.getGender());
        existing.setPhone(update.getPhone());
        existing.setEmail(update.getEmail());
        existing.setAddress(update.getAddress());
        existing.setAvatarUrl(update.getAvatarUrl());
        existing.setHeadline(update.getHeadline());
        existing.setSummary(update.getSummary());
        // program, classCode, courseName are NOT updated (immutable from LMS)

        return profileRepo.save(existing);
    }

    @Transactional
    public void upsertSelfSkills(UUID userId, List<SelfSkillDto> skills) {
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
                    userId, skill.getId(), "SELF", "SELF_" + skill.getCode())
                    .orElseGet(() -> SkillEvidence.builder()
                            .userId(userId)
                            .skill(skill)
                            .source("SELF")
                            .sourceRef("SELF_" + skill.getCode())
                            .title("Self-reported " + skill.getName())
                            .verified(false)
                            .build());

            evidence.setScore(dto.getSelfScore().shortValue());
            evidence.setRecordedAt(java.time.OffsetDateTime.now());
            evidenceRepo.save(evidence);

            // Upsert skill_score self_score
            SkillScore score = scoreRepo.findByUserIdAndSkillId(userId, skill.getId())
                    .orElseGet(() -> SkillScore.builder()
                            .userId(userId)
                            .skillId(skill.getId())
                            .level(LevelConstants.LEVEL_CO_BAN)
                            .build());

            // Update self_score (verified_score is only set by HS-01 recompute)
            score.setSelfScore(dto.getSelfScore().shortValue());
            // Level should be based on effective score = verifiedScore ?? selfScore
            Short verifiedScore = score.getVerifiedScore();
            Short effectiveScore = verifiedScore != null ? verifiedScore : dto.getSelfScore().shortValue();
            score.setLevel(computeLevel(effectiveScore));
            score.setUpdatedAt(java.time.OffsetDateTime.now());
            scoreRepo.save(score);
        }
    }

    /**
     * Compute level based on score using shared scale with HS-01.
     * Thresholds: >=85 Nâng cao, >=70 Vững, >=50 Khá, else Cơ bản.
     */
    private String computeLevel(Short score) {
        if (score == null) return LevelConstants.LEVEL_CO_BAN;
        if (score >= 85) return LevelConstants.LEVEL_NANG_CAO;
        if (score >= 70) return LevelConstants.LEVEL_VUNG;
        if (score >= 50) return LevelConstants.LEVEL_KHA;
        return LevelConstants.LEVEL_CO_BAN;
    }
}