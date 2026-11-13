package com.example.profileservice.profile.service;

import com.example.profileservice.common.LevelConstants;
import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.api.dto.UpdateProfileRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
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

    public void ensureOwnsProfile(UUID requesterId, UUID targetUserId) {
        if (requesterId == null || targetUserId == null || !requesterId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
    }

    private CandidateProfile createProfileFromLms(UUID userId) {
        LmsStudentDto lmsStudent = null;
        try {
            lmsStudent = lmsClient.getStudent(userId).block();
        } catch (Exception ex) {
            log.warn("Không thể tải thông tin sinh viên từ LMS cho userId {}: {}", userId, ex.getMessage());
        }

        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .fullName(lmsStudent != null && lmsStudent.getFullName() != null ? lmsStudent.getFullName() : "Nguyễn Văn An")
                .email(lmsStudent != null && lmsStudent.getEmail() != null ? lmsStudent.getEmail() : "candidate1@rikkeijobs.test")
                .program(lmsStudent != null && lmsStudent.getProgram() != null ? lmsStudent.getProgram() : "Lập trình Java Fullstack Chuyên sâu")
                .classCode(lmsStudent != null && lmsStudent.getClassCode() != null ? lmsStudent.getClassCode() : "RA-JAVA-2026")
                .courseName(lmsStudent != null && lmsStudent.getCourseName() != null ? lmsStudent.getCourseName() : "Khóa đào tạo Kỹ sư Phần mềm Java")
                .visibility(CandidateProfile.Visibility.PUBLIC)
                .build();

        return profileRepo.save(profile);
    }

    @Transactional
    public CandidateProfile updateProfile(UUID userId, UpdateProfileRequest update) {
        CandidateProfile existing = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));

        // Only candidate-owned fields are writable here, and a null field means "leave as is".
        // fullName, email, program, classCode and courseName are LMS-sourced and stay immutable.
        if (update.getDob() != null) existing.setDob(update.getDob());
        if (update.getGender() != null) existing.setGender(update.getGender());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getAvatarUrl() != null) existing.setAvatarUrl(update.getAvatarUrl());
        if (update.getHeadline() != null) existing.setHeadline(update.getHeadline());
        if (update.getSummary() != null) existing.setSummary(update.getSummary());
        if (update.getVisibility() != null) existing.setVisibility(update.getVisibility());

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
