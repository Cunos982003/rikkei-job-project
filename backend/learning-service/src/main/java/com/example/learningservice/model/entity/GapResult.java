package com.example.learningservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gap_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GapResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "required", nullable = false)
    private Integer required;

    @Column(name = "current", nullable = false)
    private Integer current;

    @Column(name = "gap", nullable = false)
    private Integer gap;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private GapPriority priority;

    @CreationTimestamp
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}

