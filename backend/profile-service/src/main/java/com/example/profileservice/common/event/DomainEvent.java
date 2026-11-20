package com.example.profileservice.common.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DomainEvent {
    OffsetDateTime occurredAt();
    UUID eventId();
}