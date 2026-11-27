package com.example.jobservice.match.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class MatchScoreId implements Serializable {

    private UUID jobId;
    private UUID userId;

    public MatchScoreId() {
    }

    public MatchScoreId(UUID jobId, UUID userId) {
        this.jobId = jobId;
        this.userId = userId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchScoreId that = (MatchScoreId) o;
        return Objects.equals(jobId, that.jobId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, userId);
    }
}

