package com.example.profileservice.entity.visibility;

import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.entity.ProfileReview;
import com.example.profileservice.service.InvalidTransitionException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Abstract state class for the profile visibility state machine.
 * Each concrete state knows its valid transitions and returns a StateTransition
 * result object that the Aggregate Root will apply.
 */
public abstract class VisibilityState {

    public abstract StateTransition submitForReview(CandidateProfile profile, boolean consent, UUID actorId);

    public abstract StateTransition approve(CandidateProfile profile, UUID actorId);

    public abstract StateTransition reject(CandidateProfile profile, UUID actorId, String reason);

    public abstract StateTransition withdraw(CandidateProfile profile, UUID actorId);

    protected ProfileReview createReview(UUID userId, ProfileReview.Action action, UUID actorId, String reason) {
        return ProfileReview.builder()
                .userId(userId)
                .action(action)
                .reason(reason)
                .actorId(actorId)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    protected void validateConsent(boolean consent) {
        if (!consent) {
            throw new IllegalArgumentException("Consent is required to submit profile for review");
        }
    }

    protected void validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
    }

    protected void throwInvalidTransition(String currentState, String... validStates) {
        String current = userFriendlyStateName(currentState);
        String valid = String.join(" hoặc ", java.util.Arrays.stream(validStates)
                .map(this::userFriendlyStateName)
                .toArray(String[]::new));
        throw new InvalidTransitionException(
                "Không thể thực hiện thao tác này khi hồ sơ đang ở trạng thái " + current +
                ". Chỉ có thể chuyển sang: " + valid);
    }

    private String userFriendlyStateName(String state) {
        return switch (state) {
            case "PRIVATE" -> "Riêng tư";
            case "PENDING" -> "Chờ duyệt";
            case "PUBLIC" -> "Công khai";
            case "REJECTED" -> "Bị từ chối";
            case "RECRUITER" -> "Dành cho nhà tuyển dụng";
            default -> state;
        };
    }
}