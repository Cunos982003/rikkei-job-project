package com.example.profileservice.common.event;

import com.example.profileservice.common.domain.UserId;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileUpdatedEvent(
    UUID eventId,
    OffsetDateTime occurredAt,
    UserId userId
) implements DomainEvent {
    public static ProfileUpdatedEvent of(UserId userId) {
        return new ProfileUpdatedEvent(UUID.randomUUID(), OffsetDateTime.now(), userId);
    }
}