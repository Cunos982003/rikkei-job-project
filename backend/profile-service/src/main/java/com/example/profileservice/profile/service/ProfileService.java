package com.example.profileservice.profile.service;

import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.LmsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository profileRepo;
    private final LmsClient lmsClient;

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
}