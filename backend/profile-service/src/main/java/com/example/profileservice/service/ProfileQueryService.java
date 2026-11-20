package com.example.profileservice.service;

import com.example.profileservice.common.domain.Email;
import com.example.profileservice.common.domain.Phone;
import com.example.profileservice.dto.StaffPreviewProfileDto;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.entity.Education;
import com.example.profileservice.entity.Certificate;
import com.example.profileservice.repository.CandidateProfileRepository;
import com.example.profileservice.repository.EducationRepository;
import com.example.profileservice.repository.CertificateRepository;
import com.example.profileservice.repository.ProfileReviewRepository;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillEvidence;
import com.example.profileservice.entity.SkillScore;
import com.example.profileservice.repository.SkillEvidenceRepository;
import com.example.profileservice.repository.SkillRepository;
import com.example.profileservice.repository.SkillScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for read-only profile queries and DTO mapping.
 * Extracted from ProfileService as part of service decomposition (Task 4.5).
 */
@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final CandidateProfileRepository profileRepo;
    private final EducationRepository educationRepo;
    private final CertificateRepository certificateRepo;
    private final ProfileReviewRepository reviewRepo;
    private final SkillEvidenceRepository evidenceRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;
    private final ProfileMaskingService maskingService;

    @Transactional(readOnly = true)
    public StaffPreviewProfileDto getProfileForPreview(UUID userId, long rejectionCount, List<StaffPreviewProfileDto.ReviewHistoryDto> reviewHistory) {
        CandidateProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));

        // Mask email and phone using ProfileMaskingService
        String maskedEmail = maskingService.maskEmail(Email.of(profile.getEmail()));
        String maskedPhone = maskingService.maskPhone(Phone.of(profile.getPhone()));

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
                .level(score.getLevel().label())
                .evidences(evidenceDtos)
                .build();
    }

    private StaffPreviewProfileDto.EducationDto toEducationDto(Education edu) {
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

    private StaffPreviewProfileDto.CertificateDto toCertificateDto(Certificate cert) {
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
}