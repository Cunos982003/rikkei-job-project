package com.example.profileservice.skill.domain;

import com.example.profileservice.common.domain.Score;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
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

    public static final int DECAY_THRESHOLD_DAYS = 365;
    public static final BigDecimal DECAY_FACTOR = BigDecimal.valueOf(0.5);

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

    public void updateScore(Score newScore) {
        this.score = newScore.value();
        this.recordedAt = OffsetDateTime.now();
    }

    public BigDecimal getDecayedScore() {
        if (score == null) return BigDecimal.ZERO;
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(recordedAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
        BigDecimal decay = daysDiff <= DECAY_THRESHOLD_DAYS ? BigDecimal.ONE : DECAY_FACTOR;
        return BigDecimal.valueOf(score).multiply(decay);
    }

    public boolean isRecent() {
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(recordedAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
        return daysDiff <= DECAY_THRESHOLD_DAYS;
    }
}