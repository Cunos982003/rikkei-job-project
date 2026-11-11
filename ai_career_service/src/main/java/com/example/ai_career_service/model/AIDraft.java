package com.example.ai_career_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.time.Instant;
import java.util.UUID;

import com.example.ai_career_service.enums.AIDraftSection;
import com.example.ai_career_service.enums.AIDraftStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "ai_drafts")
public class AIDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cv_id", nullable = false)
    private UUID cvId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AIDraftSection section;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "target_job_id")
    private UUID targetJobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot", nullable = false)
    private JsonNode inputSnapshot;

    @Column(nullable = false, length = 10000)
    private String output;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode warnings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AIDraftStatus status;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp 
    private Instant createdAt;
}
