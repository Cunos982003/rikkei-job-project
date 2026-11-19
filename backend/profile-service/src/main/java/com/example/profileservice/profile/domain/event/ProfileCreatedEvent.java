package com.example.profileservice.profile.domain.event;

import com.example.profileservice.common.domain.UserId;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileCreatedEvent(
    UUID eventId,
    OffsetDateTime occurredAt,
    UserId userId
) implements DomainEvent {
    public static ProfileCreatedEvent of(UserId userId) {
        return new ProfileCreatedEvent(UUID.randomUUID(), OffsetDateTime.now(), userId);
    }
}