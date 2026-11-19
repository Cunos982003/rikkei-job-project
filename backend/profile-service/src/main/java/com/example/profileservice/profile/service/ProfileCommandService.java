package com.example.profileservice.profile.service;

import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.api.dto.UpdateProfileRequest;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for profile command operations (create, update).
 * Extracted from ProfileService as part of service decomposition (Task 4.1).
 */
@Service
@RequiredArgsConstructor
public class ProfileCommandService {

    private final CandidateProfileRepository profileRepo;
    private final LmsProfileService lmsProfileService;

    @Transactional
    public CandidateProfile getOrCreateProfile(UUID userId) {
        return profileRepo.findByUserId(userId)
                .orElseGet(() -> createProfileWithFallback(userId));
    }

    @Transactional
    public CandidateProfile getOrCreateProfile(UserId userId) {
        return getOrCreateProfile(userId.value());
    }

    /**
     * Create profile with fallback to minimal profile if LMS is unavailable or returns 404.
     * LMS call is outside transaction to avoid holding DB connections during external calls.
     */
    private CandidateProfile createProfileWithFallback(UUID userId) {
        // Try to fetch from LMS first (outside transaction)
        LmsStudentDto lmsStudent = lmsProfileService.fetchStudent(userId);

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
            profile = lmsProfileService.createMinimalProfile(UserId.of(userId));
        }

        return profileRepo.save(profile);
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
    public CandidateProfile updateProfile(UserId userId, UpdateProfileRequest update) {
        return updateProfile(userId.value(), update);
    }
}