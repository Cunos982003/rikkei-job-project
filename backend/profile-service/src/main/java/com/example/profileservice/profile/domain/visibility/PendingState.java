package com.example.profileservice.profile.domain.visibility;

import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.service.InvalidTransitionException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * PENDING state - profile is under review by staff.
 * Can transition to: PUBLIC (via approve), REJECTED (via reject), PRIVATE (via withdraw)
 */
public class PendingState extends VisibilityState {

    @Override
    public StateTransition submitForReview(CandidateProfile profile, boolean consent, UUID actorId) {
        throwInvalidTransition("PENDING", "PRIVATE, REJECTED");
        return null; // unreachable
    }

    @Override
    public StateTransition approve(CandidateProfile profile, UUID actorId) {
        ProfileReview review = createReview(profile.getUserId(), ProfileReview.Action.APPROVE, actorId, null);
        return StateTransition.withVisibilityAndReview(Visibility.PUBLIC, review);
    }

    @Override
    public StateTransition reject(CandidateProfile profile, UUID actorId, String reason) {
        validateReason(reason);
        ProfileReview review = createReview(profile.getUserId(), ProfileReview.Action.REJECT, actorId, reason);
        return StateTransition.withVisibilityAndReview(Visibility.REJECTED, review);
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