package com.example.profileservice.internship.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "internship_tasks")
public class InternshipTask {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_id", nullable = false)
    private Internship internship;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "accepted_score")
    private BigDecimal acceptedScore;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    // --- GETTERS VÀ SETTERS ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Internship getInternship() {
        return internship;
    }

    public void setInternship(Internship internship) {
        this.internship = internship;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAcceptedScore() {
        return acceptedScore;
    }

    public void setAcceptedScore(BigDecimal acceptedScore) {
        this.acceptedScore = acceptedScore;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(OffsetDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}