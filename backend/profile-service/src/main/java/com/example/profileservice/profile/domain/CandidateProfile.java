package com.example.profileservice.profile.domain;

import com.example.profileservice.profile.domain.visibility.VisibilityState;
import com.example.profileservice.profile.domain.visibility.VisibilityStateFactory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileReview> reviews = new ArrayList<>();

    @Transient
    private VisibilityState currentState;

    public enum Visibility {
        PRIVATE, PENDING, PUBLIC, REJECTED, RECRUITER
    }

    @PostLoad
    @PostPersist
    private void initializeState() {
        this.currentState = VisibilityStateFactory.getState(this.visibility);
    }

    /**
     * Ensure the current state is initialized.
     * This is needed for unit tests where JPA callbacks don't fire.
     */
    private void ensureStateInitialized() {
        if (this.currentState == null) {
            this.currentState = VisibilityStateFactory.getState(this.visibility);
        }
    }

    // ==================== State Machine Methods ====================

    /**
     * Submit profile for review (PRIVATE/REJECTED -> PENDING).
     * Candidate must provide consent.
     */
    public void submitForReview(boolean consent, UUID actorId) {
        ensureStateInitialized();
        currentState.submitForReview(this, consent, actorId);
        this.currentState = VisibilityStateFactory.getState(this.visibility);
    }

    /**
     * Approve profile (PENDING -> PUBLIC).
     * Staff only.
     */
    public void approve(UUID actorId) {
        ensureStateInitialized();
        currentState.approve(this, actorId);
        this.currentState = VisibilityStateFactory.getState(this.visibility);
    }

    /**
     * Reject profile (PENDING -> REJECTED).
     * Staff only. Requires reason.
     */
    public void reject(UUID actorId, String reason) {
        ensureStateInitialized();
        currentState.reject(this, actorId, reason);
        this.currentState = VisibilityStateFactory.getState(this.visibility);
    }

    /**
     * Withdraw profile (PENDING/PUBLIC -> PRIVATE).
     * Candidate only.
     */
    public void withdraw(UUID actorId) {
        ensureStateInitialized();
        currentState.withdraw(this, actorId);
        this.currentState = VisibilityStateFactory.getState(this.visibility);
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
}