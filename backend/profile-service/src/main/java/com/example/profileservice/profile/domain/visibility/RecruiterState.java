package com.example.profileservice.profile.domain.visibility;

import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.service.InvalidTransitionException;

import java.util.UUID;

/**
 * RECRUITER state - profile is visible only to specific recruiters.
 * Not part of the HS-05 workflow, but kept for backward compatibility.
 */
public class RecruiterState extends VisibilityState {

    @Override
    public StateTransition submitForReview(CandidateProfile profile, boolean consent, UUID actorId) {
        throwInvalidTransition("RECRUITER", "PRIVATE, REJECTED");
        return null; // unreachable
    }

    @Override
    public StateTransition approve(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("RECRUITER", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition reject(CandidateProfile profile, UUID actorId, String reason) {
        throwInvalidTransition("RECRUITER", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition withdraw(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("RECRUITER", "PENDING, PUBLIC");
        return null; // unreachable
    }
}