package com.example.profileservice.service;

import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.dto.LmsStudentDto;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.client.LmsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application service for LMS integration.
 * Extracted from ProfileCommandService as part of service decomposition (Task 4.3).
 */
@Service
@RequiredArgsConstructor
public class LmsProfileService {

    private final LmsClient lmsClient;

    /**
     * Fetch student from LMS, returning null for 404 or any error.
     * This method runs outside transaction boundary.
     */
    public LmsStudentDto fetchStudent(UserId userId) {
        return fetchStudent(userId.value());
    }

    public LmsStudentDto fetchStudent(UUID userId) {
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

    /**
     * Create minimal profile with just userId (fallback when LMS is unavailable or user not found).
     */
    public CandidateProfile createMinimalProfile(UserId userId) {
        return CandidateProfile.builder()
                .userId(userId.value())
                .build();
    }
}