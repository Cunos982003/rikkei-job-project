package com.example.profileservice.common.event;

import com.example.profileservice.profile.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Domain event publisher that wraps Spring's ApplicationEventPublisher.
 * Created as part of Phase 6 (Task 6.1) to publish domain events.
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Publish a domain event.
     * @param event the domain event to publish
     */
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}