package com.example.jobservice.match.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_scores")
@IdClass(MatchScoreId.class)
public class MatchScore {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "score", nullable = false, precision = 5, scale = 1)
    private BigDecimal score;

    @Column(name = "below_minimum", nullable = false)
    private boolean belowMinimum;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "stale", nullable = false)
    private boolean stale;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    public MatchScore() {
    }

    public MatchScore(UUID jobId, UUID userId, BigDecimal score, boolean belowMinimum, String detail, boolean stale, OffsetDateTime computedAt) {
        this.jobId = jobId;
        this.userId = userId;
        this.score = score;
        this.belowMinimum = belowMinimum;
        this.detail = detail;
        this.stale = stale;
        this.computedAt = computedAt;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public boolean isBelowMinimum() {
        return belowMinimum;
    }

    public void setBelowMinimum(boolean belowMinimum) {
        this.belowMinimum = belowMinimum;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(OffsetDateTime computedAt) {
        this.computedAt = computedAt;
    }
}

