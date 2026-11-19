package com.example.profileservice.profile.domain.visibility;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.service.InvalidTransitionException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * PRIVATE state - profile is not visible to anyone.
 * Can transition to: PENDING (via submitForReview)
 */
public class PrivateState extends VisibilityState {

    @Override
    public StateTransition submitForReview(CandidateProfile profile, boolean consent, UUID actorId) {
        validateConsent(consent);

        ProfileReview review = createReview(profile.getUserId(), ProfileReview.Action.SUBMIT, actorId, null);
        return StateTransition.withAll(
                Visibility.PENDING,
                OffsetDateTime.now(),
                null,
                review
        );
    }

    @Override
    public StateTransition approve(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("PRIVATE", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition reject(CandidateProfile profile, UUID actorId, String reason) {
        throwInvalidTransition("PRIVATE", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition withdraw(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("PRIVATE", "PENDING, PUBLIC");
        return null; // unreachable
    }
}