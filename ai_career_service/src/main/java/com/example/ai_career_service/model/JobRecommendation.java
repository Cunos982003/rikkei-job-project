package com.example.ai_career_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
        name = "job_recommendations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_recommendations_user_job",
                columnNames = {"user_id", "job_id"}))
public class JobRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp 
    private Instant createdAt;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;
}
