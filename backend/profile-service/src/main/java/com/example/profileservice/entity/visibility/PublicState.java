package com.example.profileservice.entity.visibility;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.entity.ProfileReview;
import com.example.profileservice.service.InvalidTransitionException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * PUBLIC state - profile is visible to enterprises/recruiters.
 * Can transition to: PRIVATE (via withdraw)
 */
public class PublicState extends VisibilityState {

    @Override
    public StateTransition submitForReview(CandidateProfile profile, boolean consent, UUID actorId) {
        throwInvalidTransition("PUBLIC", "PRIVATE, REJECTED");
        return null; // unreachable
    }

    @Override
    public StateTransition approve(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("PUBLIC", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition reject(CandidateProfile profile, UUID actorId, String reason) {
        throwInvalidTransition("PUBLIC", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition withdraw(CandidateProfile profile, UUID actorId) {
        ProfileReview review = createReview(profile.getUserId(), ProfileReview.Action.WITHDRAW, actorId, null);
        return StateTransition.withAll(
                Visibility.PRIVATE,
                null,
                OffsetDateTime.now(),
                review
        );
    }
}