package com.example.profileservice.profile.domain.visibility;

import com.example.profileservice.common.domain.Visibility;

/**
 * Factory to get the appropriate VisibilityState instance for a given visibility.
 *
 * <p><strong>Thread Safety:</strong> All concrete state classes ({@link PrivateState}, {@link PendingState},
 * {@link PublicState}, {@link RejectedState}, {@link RecruiterState}) are stateless - they contain no instance
 * fields and only delegate to the {@link CandidateProfile} aggregate for state mutations. Therefore, singleton
 * instances are safe to share across threads. If any state class is modified to hold state in the future,
 * this factory must be changed to return new instances instead of singletons.
 */
public class VisibilityStateFactory {

    private static final VisibilityState PRIVATE_STATE = new PrivateState();
    private static final VisibilityState PENDING_STATE = new PendingState();
    private static final VisibilityState PUBLIC_STATE = new PublicState();
    private static final VisibilityState REJECTED_STATE = new RejectedState();
    private static final VisibilityState RECRUITER_STATE = new RecruiterState();

    private VisibilityStateFactory() {}

    public static VisibilityState getState(Visibility visibility) {
        return switch (visibility) {
            case PRIVATE -> PRIVATE_STATE;
            case PENDING -> PENDING_STATE;
            case PUBLIC -> PUBLIC_STATE;
            case REJECTED -> REJECTED_STATE;
            case RECRUITER -> RECRUITER_STATE;
        };
    }
}