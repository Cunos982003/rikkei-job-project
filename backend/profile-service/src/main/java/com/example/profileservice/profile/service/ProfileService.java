package com.example.profileservice.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Legacy ProfileService - kept for backward compatibility during service decomposition.
 * All methods have been moved to:
 * - ProfileCommandService (profile CRUD)
 * - SkillProfileService (self-skills management)
 * - LmsProfileService (LMS integration)
 * - ProfileQueryService (read-only queries and DTO mapping)
 * - ProfileMaskingService (email/phone masking)
 *
 * This class will be deleted in Task 4.6.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {
    // Intentionally empty - all functionality moved to dedicated services
}