package com.example.profileservice.profile.domain.visibility;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.service.InvalidTransitionException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REJECTED state - profile was rejected by staff.
 * Can transition to: PENDING (via submitForReview - resubmit after fixing)
 */
public class RejectedState extends VisibilityState {

    @Override
    public StateTransition submitForReview(CandidateProfile profile, boolean consent, UUID actorId) {
        validateConsent(consent);

        ProfileReview review = createReview(profile.getUserId(), ProfileReview.Action.SUBMIT, actorId, null);
        return StateTransition.withAllAndClearConsentWithdrawn(
                Visibility.PENDING,
                OffsetDateTime.now(),
                review
        );
    }

    @Override
    public StateTransition approve(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("REJECTED", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition reject(CandidateProfile profile, UUID actorId, String reason) {
        throwInvalidTransition("REJECTED", "PENDING");
        return null; // unreachable
    }

    @Override
    public StateTransition withdraw(CandidateProfile profile, UUID actorId) {
        throwInvalidTransition("REJECTED", "PENDING, PUBLIC");
        return null; // unreachable
    }
}