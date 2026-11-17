package com.example.profileservice.profile.service;

import com.example.profileservice.common.LevelConstants;
import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.api.dto.StaffPreviewProfileDto;
import com.example.profileservice.profile.api.dto.UpdateProfileRequest;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.EducationRepository;
import com.example.profileservice.profile.infrastructure.CertificateRepository;
import com.example.profileservice.profile.infrastructure.LmsClient;
import com.example.profileservice.profile.infrastructure.ProfileReviewRepository;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository profileRepo;
    private final EducationRepository educationRepo;
    private final CertificateRepository certificateRepo;
    private final ProfileReviewRepository reviewRepo;
    private final LmsClient lmsClient;
    private final SkillEvidenceRepository evidenceRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public CandidateProfile getOrCreateProfile(UUID userId) {
        return profileRepo.findByUserId(userId)
                .orElseGet(() -> createProfileWithFallback(userId));
    }

    /**
     * Create profile with fallback to minimal profile if LMS is unavailable or returns 404.
     * LMS call is outside transaction to avoid holding DB connections during external calls.
     */
    private CandidateProfile createProfileWithFallback(UUID userId) {
        // Try to fetch from LMS first (outside transaction)
        LmsStudentDto lmsStudent = fetchStudentFromLms(userId);

        CandidateProfile profile;
        if (lmsStudent != null) {
            profile = CandidateProfile.builder()
                    .userId(userId)
                    .fullName(lmsStudent.getFullName())
                    .email(lmsStudent.getEmail())
                    .program(lmsStudent.getProgram())
                    .classCode(lmsStudent.getClassCode())
                    .courseName(lmsStudent.getCourseName())
                    .build();
        } else {
            // Fallback: create minimal profile with just userId
            // This allows candidate to proceed even if LMS is down or user not in LMS yet
            profile = CandidateProfile.builder()
                    .userId(userId)
                    .build();
        }

        return profileRepo.save(profile);
    }

    /**
     * Fetch student from LMS, returning null for 404 or any error.
     * This method runs outside transaction boundary.
     */
    private LmsStudentDto fetchStudentFromLms(UUID userId) {
        try {
            return lmsClient.getStudent(userId).block();
        } catch (LmsClient.LmsClientException e) {
            // Log the error but don't propagate - we'll create minimal profile as fallback
            // TODO: Add proper logging (e.g., logger.warn("LMS call failed for userId: {}", userId, e));
            return null;
        } catch (Exception e) {
            // Catch any other unexpected errors (network, timeout, etc.)
            // TODO: Add proper logging
            return null;
        }
    }

    @Transactional
    public CandidateProfile updateProfile(UUID userId, UpdateProfileRequest update) {
        CandidateProfile existing = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));

        // Only candidate-owned fields are writable here, and a null field means "leave as is".
        // fullName, email, program, classCode and courseName are LMS-sourced and stay immutable.
        // visibility is deliberately absent: it only changes through the review workflow.
        if (update.getDob() != null) existing.setDob(update.getDob());
        if (update.getGender() != null) existing.setGender(update.getGender());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getAvatarUrl() != null) existing.setAvatarUrl(update.getAvatarUrl());
        if (update.getHeadline() != null) existing.setHeadline(update.getHeadline());
        if (update.getSummary() != null) existing.setSummary(update.getSummary());

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

    /**
     * Get full profile for staff preview with masked contact info.
     * Used by VisibilityService.getPreviewProfile().
     */
    @Transactional(readOnly = true)
    public StaffPreviewProfileDto getProfileForPreview(UUID userId, long rejectionCount, List<StaffPreviewProfileDto.ReviewHistoryDto> reviewHistory) {
        CandidateProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));

        // Mask email and phone (UT-03 fallback: simple masking)
        String maskedEmail = maskEmail(profile.getEmail());
        String maskedPhone = maskPhone(profile.getPhone());

        // Get skills with evidences
        List<SkillScore> skillScores = scoreRepo.findByUserId(userId);
        List<StaffPreviewProfileDto.SkillProfileDto> skills = skillScores.stream()
                .map(this::toSkillProfileDto)
                .toList();

        // Get educations
        List<StaffPreviewProfileDto.EducationDto> educations = educationRepo.findByUserId(userId).stream()
                .map(this::toEducationDto)
                .toList();

        // Get certificates
        List<StaffPreviewProfileDto.CertificateDto> certificates = certificateRepo.findByUserId(userId).stream()
                .map(this::toCertificateDto)
                .toList();

        return StaffPreviewProfileDto.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .maskedEmail(maskedEmail)
                .maskedPhone(maskedPhone)
                .dob(profile.getDob() != null ? profile.getDob().toString() : null)
                .gender(profile.getGender())
                .address(profile.getAddress())
                .avatarUrl(profile.getAvatarUrl())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .program(profile.getProgram())
                .classCode(profile.getClassCode())
                .courseName(profile.getCourseName())
                .visibility(profile.getVisibility())
                .consentAt(profile.getConsentAt())
                .consentWithdrawnAt(profile.getConsentWithdrawnAt())
                .updatedAt(profile.getUpdatedAt())
                .skills(skills)
                .educations(educations)
                .certificates(certificates)
                .reviewHistory(reviewHistory)
                .rejectionCount((int) rejectionCount)
                .build();
    }

    /**
     * Upsert self-skills from JSON string (for staff edit self_skills).
     * JSON format: [{"skillId": "uuid", "selfScore": 80}, ...]
     */
    @Transactional
    public void upsertSelfSkillsFromJson(UUID userId, String json) {
        try {
            List<SelfSkillDto> skills = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SelfSkillDto.class));
            upsertSelfSkills(userId, skills);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid self_skills JSON: " + e.getMessage(), e);
        }
    }

    // ==================== Helper methods for DTO mapping ====================

    private StaffPreviewProfileDto.SkillProfileDto toSkillProfileDto(SkillScore score) {
        Skill skill = skillRepo.findById(score.getSkillId())
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + score.getSkillId()));

        List<SkillEvidence> evidences = evidenceRepo.findByUserIdAndSkillId(score.getUserId(), skill.getId());
        List<StaffPreviewProfileDto.SkillProfileDto.SkillEvidenceDto> evidenceDtos = evidences.stream()
                .map(e -> StaffPreviewProfileDto.SkillProfileDto.SkillEvidenceDto.builder()
                        .source(e.getSource())
                        .sourceRef(e.getSourceRef())
                        .title(e.getTitle())
                        .score(e.getScore() != null ? e.getScore().intValue() : null)
                        .recordedAt(e.getRecordedAt())
                        .verified(e.isVerified())
                        .build())
                .toList();

        return StaffPreviewProfileDto.SkillProfileDto.builder()
                .skillId(skill.getId())
                .skillCode(skill.getCode())
                .skillName(skill.getName())
                .group(skill.getGroup())
                .verifiedScore(score.getVerifiedScore() != null ? score.getVerifiedScore().intValue() : null)
                .selfScore(score.getSelfScore() != null ? score.getSelfScore().intValue() : null)
                .level(score.getLevel())
                .evidences(evidenceDtos)
                .build();
    }

    private StaffPreviewProfileDto.EducationDto toEducationDto(com.example.profileservice.profile.domain.Education edu) {
        return StaffPreviewProfileDto.EducationDto.builder()
                .id(edu.getId())
                .school(edu.getSchool())
                .major(edu.getMajor())
                .fromDate(edu.getFromDate() != null ? edu.getFromDate().toString() : null)
                .toDate(edu.getToDate() != null ? edu.getToDate().toString() : null)
                .gpa(edu.getGpa() != null ? edu.getGpa().doubleValue() : null)
                .note(edu.getNote())
                .build();
    }

    private StaffPreviewProfileDto.CertificateDto toCertificateDto(com.example.profileservice.profile.domain.Certificate cert) {
        return StaffPreviewProfileDto.CertificateDto.builder()
                .id(cert.getId())
                .name(cert.getName())
                .issuer(cert.getIssuer())
                .issuedAt(cert.getIssuedAt() != null ? cert.getIssuedAt().toString() : null)
                .expiresAt(cert.getExpiresAt() != null ? cert.getExpiresAt().toString() : null)
                .fileUrl(cert.getFileUrl())
                .fileType(cert.getFileType())
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 3) return "***@" + domain;
        return local.substring(0, 3) + "***@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 1);
    }
}