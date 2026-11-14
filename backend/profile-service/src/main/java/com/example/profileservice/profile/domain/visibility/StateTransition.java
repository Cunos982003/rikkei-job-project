package com.example.profileservice.profile.domain.visibility;

import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.domain.ProfileReview;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Result object representing a state transition.
 * Encapsulates all changes that should be applied to the CandidateProfile aggregate.
 * This keeps state mutation logic inside the Aggregate Root, maintaining DDD encapsulation.
 */
public record StateTransition(
        Optional<Visibility> newVisibility,
        Optional<OffsetDateTime> consentAt,
        Optional<OffsetDateTime> consentWithdrawnAt,
        Optional<ProfileReview> reviewToAdd,
        boolean clearConsentAt,
        boolean clearConsentWithdrawnAt
) {
    public static StateTransition none() {
        return new StateTransition(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false, false);
    }

    public static StateTransition of(Visibility newVisibility, OffsetDateTime consentAt, OffsetDateTime consentWithdrawnAt, ProfileReview reviewToAdd) {
        return new StateTransition(
                Optional.ofNullable(newVisibility),
                Optional.ofNullable(consentAt),
                Optional.ofNullable(consentWithdrawnAt),
                Optional.ofNullable(reviewToAdd),
                false, false
        );
    }

    public static StateTransition withVisibility(Visibility newVisibility) {
        return new StateTransition(Optional.of(newVisibility), Optional.empty(), Optional.empty(), Optional.empty(), false, false);
    }

    public static StateTransition withReview(ProfileReview review) {
        return new StateTransition(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(review), false, false);
    }

    public static StateTransition withVisibilityAndReview(Visibility newVisibility, ProfileReview review) {
        return new StateTransition(Optional.of(newVisibility), Optional.empty(), Optional.empty(), Optional.of(review), false, false);
    }

    public static StateTransition withTimestamps(OffsetDateTime consentAt, OffsetDateTime consentWithdrawnAt) {
        return new StateTransition(Optional.empty(), Optional.ofNullable(consentAt), Optional.ofNullable(consentWithdrawnAt), Optional.empty(), false, false);
    }

    public static StateTransition withVisibilityAndTimestamps(Visibility newVisibility, OffsetDateTime consentAt, OffsetDateTime consentWithdrawnAt) {
        return new StateTransition(Optional.of(newVisibility), Optional.ofNullable(consentAt), Optional.ofNullable(consentWithdrawnAt), Optional.empty(), false, false);
    }

    public static StateTransition withAll(Visibility newVisibility, OffsetDateTime consentAt, OffsetDateTime consentWithdrawnAt, ProfileReview review) {
        return new StateTransition(Optional.of(newVisibility), Optional.ofNullable(consentAt), Optional.ofNullable(consentWithdrawnAt), Optional.of(review), false, false);
    }

    public static StateTransition withAllAndClearConsentWithdrawn(Visibility newVisibility, OffsetDateTime consentAt, ProfileReview review) {
        return new StateTransition(Optional.of(newVisibility), Optional.ofNullable(consentAt), Optional.empty(), Optional.of(review), false, true);
    }

    public boolean shouldClearConsentAt() {
        return clearConsentAt;
    }

    public boolean shouldClearConsentWithdrawnAt() {
        return clearConsentWithdrawnAt;
    }
}