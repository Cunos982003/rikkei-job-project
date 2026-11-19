package com.example.profileservice.profile.domain;

import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.domain.event.DomainEvent;
import com.example.profileservice.profile.domain.event.ProfileVisibilityChangedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateProfileDomainEventTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    void submitForReview_emitsVisibilityChangedEvent() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PRIVATE)
            .build();

        profile.submitForReview(true, ACTOR_ID);

        List<DomainEvent> events = profile.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ProfileVisibilityChangedEvent.class);

        ProfileVisibilityChangedEvent e = (ProfileVisibilityChangedEvent) events.get(0);
        assertThat(e.userId().value()).isEqualTo(USER_ID);
        assertThat(e.oldVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(e.newVisibility()).isEqualTo(Visibility.PENDING);
    }

    @Test
    void approve_emitsVisibilityChangedEvent() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PENDING)
            .build();

        profile.approve(ACTOR_ID);

        List<DomainEvent> events = profile.getDomainEvents();
        assertThat(events).hasSize(1);

        ProfileVisibilityChangedEvent e = (ProfileVisibilityChangedEvent) events.get(0);
        assertThat(e.userId().value()).isEqualTo(USER_ID);
        assertThat(e.oldVisibility()).isEqualTo(Visibility.PENDING);
        assertThat(e.newVisibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    void reject_emitsVisibilityChangedEvent() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PENDING)
            .build();

        profile.reject(ACTOR_ID, "Thiếu minh chứng");

        List<DomainEvent> events = profile.getDomainEvents();
        assertThat(events).hasSize(1);

        ProfileVisibilityChangedEvent e = (ProfileVisibilityChangedEvent) events.get(0);
        assertThat(e.userId().value()).isEqualTo(USER_ID);
        assertThat(e.oldVisibility()).isEqualTo(Visibility.PENDING);
        assertThat(e.newVisibility()).isEqualTo(Visibility.REJECTED);
    }

    @Test
    void withdraw_emitsVisibilityChangedEvent() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PENDING)
            .build();

        profile.withdraw(ACTOR_ID);

        List<DomainEvent> events = profile.getDomainEvents();
        assertThat(events).hasSize(1);

        ProfileVisibilityChangedEvent e = (ProfileVisibilityChangedEvent) events.get(0);
        assertThat(e.userId().value()).isEqualTo(USER_ID);
        assertThat(e.oldVisibility()).isEqualTo(Visibility.PENDING);
        assertThat(e.newVisibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void withdraw_fromPublic_emitsVisibilityChangedEvent() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PUBLIC)
            .build();

        profile.withdraw(ACTOR_ID);

        List<DomainEvent> events = profile.getDomainEvents();
        assertThat(events).hasSize(1);

        ProfileVisibilityChangedEvent e = (ProfileVisibilityChangedEvent) events.get(0);
        assertThat(e.newVisibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void clearDomainEvents_removesEvents() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PRIVATE)
            .build();

        profile.submitForReview(true, ACTOR_ID);
        assertThat(profile.getDomainEvents()).hasSize(1);

        profile.clearDomainEvents();
        assertThat(profile.getDomainEvents()).isEmpty();
    }

    @Test
    void multipleTransitions_accumulateEvents() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PRIVATE)
            .build();

        profile.submitForReview(true, ACTOR_ID);
        profile.reject(ACTOR_ID, "Lý do");
        profile.submitForReview(true, ACTOR_ID);
        profile.approve(ACTOR_ID);

        List<DomainEvent> events = profile.getDomainEvents();
        assertThat(events).hasSize(4);
    }

    @Test
    void invalidTransition_noEventEmitted() {
        CandidateProfile profile = CandidateProfile.builder()
            .userId(USER_ID)
            .visibility(Visibility.PUBLIC)
            .build();

        try {
            profile.submitForReview(true, ACTOR_ID);
        } catch (Exception ignored) {
            // Expected: InvalidTransitionException
        }

        assertThat(profile.getDomainEvents()).isEmpty();
        assertThat(profile.getVisibility()).isEqualTo(Visibility.PUBLIC);
    }
}