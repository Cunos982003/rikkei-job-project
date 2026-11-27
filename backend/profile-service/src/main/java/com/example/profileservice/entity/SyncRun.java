package com.example.profileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "trigger", nullable = false, length = 20)
    private String trigger; // 'NIGHTLY', 'ON_OPEN', 'MANUAL'

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // 'RUNNING', 'SUCCESS', 'FAILED'

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "counts", columnDefinition = "jsonb")
    private String counts; // e.g. {"total":10,"lms":4,"exam":3,"pm":3}
}
