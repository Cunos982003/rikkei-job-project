package com.example.profileservice.entity;

import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.domain.event.DomainEvent;
import com.example.profileservice.profile.domain.event.ProfileCreatedEvent;
import com.example.profileservice.profile.domain.event.ProfileUpdatedEvent;
import com.example.profileservice.profile.domain.event.ProfileVisibilityChangedEvent;
import com.example.profileservice.profile.domain.visibility.StateTransition;
import com.example.profileservice.profile.domain.visibility.VisibilityState;
import com.example.profileservice.profile.domain.visibility.VisibilityStateFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.profileservice.common.event.DomainEvent;
import com.example.profileservice.common.event.ProfileCreatedEvent;
import com.example.profileservice.common.event.ProfileUpdatedEvent;
import com.example.profileservice.common.event.ProfileVisibilityChangedEvent;
import com.example.profileservice.entity.visibility.StateTransition;
import com.example.profileservice.entity.visibility.VisibilityState;
import com.example.profileservice.entity.visibility.VisibilityStateFactory;
import jakarta.persistence.*;
import jakarta.persistence.Transient;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfile {

    @Id
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "dob")
    private java.time.LocalDate dob;

    @Column(length = 20)
    private String gender;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 100)
    private String program;

    @Column(name = "class_code", length = 50)
    private String classCode;

    @Column(name = "course_name", length = 200)
    private String courseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "consent_at")
    private OffsetDateTime consentAt;

    @Column(name = "consent_withdrawn_at")
    private OffsetDateTime consentWithdrawnAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileReview> reviews = new ArrayList<>();

    @JsonIgnore
    @Transient
    @Builder.Default
    private List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Get the current state object for the profile's visibility.
     * Computed on-demand to avoid JPA callback reliance and transient field issues.
     */
    private VisibilityState getCurrentState() {
        return VisibilityStateFactory.getState(this.visibility);
    }

    // ==================== State Machine Methods ====================

    /**
     * Submit profile for review (PRIVATE/REJECTED -> PENDING).
     * Candidate must provide consent.
     */
    public void submitForReview(boolean consent, UUID actorId) {
        Visibility oldVisibility = this.visibility;
        applyTransition(getCurrentState().submitForReview(this, consent, actorId));
        if (this.visibility != oldVisibility) {
            emitVisibilityChanged(oldVisibility, this.visibility);
        }
    }

    /**
     * Approve profile (PENDING -> PUBLIC).
     * Staff only.
     */
    public void approve(UUID actorId) {
        Visibility oldVisibility = this.visibility;
        applyTransition(getCurrentState().approve(this, actorId));
        if (this.visibility != oldVisibility) {
            emitVisibilityChanged(oldVisibility, this.visibility);
        }
    }

    /**
     * Reject profile (PENDING -> REJECTED).
     * Staff only. Requires reason.
     */
    public void reject(UUID actorId, String reason) {
        Visibility oldVisibility = this.visibility;
        applyTransition(getCurrentState().reject(this, actorId, reason));
        if (this.visibility != oldVisibility) {
            emitVisibilityChanged(oldVisibility, this.visibility);
        }
    }

    /**
     * Withdraw profile (PENDING/PUBLIC -> PRIVATE).
     * Candidate only.
     */
    public void withdraw(UUID actorId) {
        Visibility oldVisibility = this.visibility;
        applyTransition(getCurrentState().withdraw(this, actorId));
        if (this.visibility != oldVisibility) {
            emitVisibilityChanged(oldVisibility, this.visibility);
        }
    }

    /**
     * Apply a state transition to this aggregate.
     * Encapsulates all state mutations within the Aggregate Root.
     */
    private void applyTransition(StateTransition transition) {
        transition.newVisibility().ifPresent(v -> this.visibility = v);
        transition.consentAt().ifPresent(this::setConsentAt);
        transition.consentWithdrawnAt().ifPresent(this::setConsentWithdrawnAt);
        if (transition.shouldClearConsentAt()) {
            this.consentAt = null;
        }
        if (transition.shouldClearConsentWithdrawnAt()) {
            this.consentWithdrawnAt = null;
        }
        transition.reviewToAdd().ifPresent(this::addReview);
    }

    private void emitVisibilityChanged(Visibility oldVisibility, Visibility newVisibility) {
        UserId userId = UserId.of(this.userId);
        domainEvents.add(ProfileVisibilityChangedEvent.of(userId, oldVisibility, newVisibility));
    }

    // ==================== Internal Methods for State Classes ====================

    /**
     * Add a review to the profile's review history.
     * Called by state classes.
     */
    public void addReview(ProfileReview review) {
        review.setProfile(this);
        this.reviews.add(review);
    }

    /**
     * Get review history ordered by creation date (newest first).
     */
    public List<ProfileReview> getReviewHistory() {
        return this.reviews.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .toList();
    }

    /**
     * Get the latest SUBMIT review (for staff dashboard).
     */
    public OffsetDateTime getLatestSubmittedAt() {
        return this.reviews.stream()
                .filter(r -> r.getAction() == ProfileReview.Action.SUBMIT)
                .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .map(ProfileReview::getCreatedAt)
                .orElse(null);
    }

    // ==================== Domain Events ====================

    /**
     * Get all domain events emitted by this aggregate.
     */
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    /**
     * Clear domain events after they have been published.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}