package com.example.profileservice.skill.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill_evidences", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_id", "source", "source_ref"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillEvidence {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false, length = 20)
    private String source; // LMS, EXAM, INTERNSHIP, MENTOR, SELF

    @Column(name = "source_ref", nullable = false, length = 100)
    private String sourceRef;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Short score; // 0-100

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(nullable = false)
    private boolean verified;
}