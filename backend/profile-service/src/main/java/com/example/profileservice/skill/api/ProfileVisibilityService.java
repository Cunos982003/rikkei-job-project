package com.example.profileservice.skill.api;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProfileVisibilityService {

    /**
     * Checks if the requester can view the target user's profile.
     * For now, returns true only if requesterId equals targetUserId (own profile).
     * In a real implementation, this would check consent + approval status.
     * Returns false to trigger 404 (not 403) to avoid user enumeration.
     */
    public boolean canViewProfile(UUID requesterId, UUID targetUserId) {
        // Own profile is always visible
        if (requesterId.equals(targetUserId)) {
            return true;
        }
        // For other users' profiles, check consent/approval (not implemented yet)
        // Return false to trigger 404 (not 403) to avoid user enumeration
        return false;
    }
}