package com.example.profileservice.common.event;

import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.common.domain.Visibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileVisibilityChangedEvent(
    UUID eventId,
    OffsetDateTime occurredAt,
    UserId userId,
    Visibility oldVisibility,
    Visibility newVisibility
) implements DomainEvent {
    public static ProfileVisibilityChangedEvent of(UserId userId, Visibility oldVisibility, Visibility newVisibility) {
        return new ProfileVisibilityChangedEvent(
            UUID.randomUUID(),
            OffsetDateTime.now(),
            userId,
            oldVisibility,
            newVisibility
        );
    }
}