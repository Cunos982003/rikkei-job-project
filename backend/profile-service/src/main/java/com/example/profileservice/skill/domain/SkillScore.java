package com.example.profileservice.skill.domain;

import com.example.profileservice.common.domain.Level;
import com.example.profileservice.common.domain.Score;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "skill_scores")
@IdClass(SkillScoreId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillScore {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Id
    @Column(name = "skill_id", columnDefinition = "uuid")
    private UUID skillId;

    @Column(name = "verified_score")
    private Short verifiedScore;

    @Column(name = "self_score")
    private Short selfScore;

    @Column(nullable = false, length = 20)
    private String level; // Cơ bản, Khá, Vững, Nâng cao

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public void recalculate(List<SkillEvidence> evidences) {
        List<SkillEvidence> verified = evidences.stream()
            .filter(SkillEvidence::isVerified)
            .collect(Collectors.toList());
        List<SkillEvidence> self = evidences.stream()
            .filter(e -> !e.isVerified())
            .collect(Collectors.toList());

        // Calculate verified_score: average of decayed scores
        if (!verified.isEmpty()) {
            BigDecimal totalDecayedScore = BigDecimal.ZERO;
            for (SkillEvidence evidence : verified) {
                totalDecayedScore = totalDecayedScore.add(evidence.getDecayedScore());
            }
            BigDecimal avg = totalDecayedScore.divide(BigDecimal.valueOf(verified.size()), 0, BigDecimal.ROUND_HALF_UP);
            this.verifiedScore = avg.shortValueExact();
        } else {
            this.verifiedScore = null;
        }

        // Calculate self_score: simple average
        if (!self.isEmpty()) {
            double avg = self.stream().mapToInt(SkillEvidence::getScore).average().orElse(0);
            this.selfScore = (short) Math.round(avg);
        } else {
            this.selfScore = null;
        }

        // Determine effective score and level
        Short effectiveScore = this.verifiedScore != null ? this.verifiedScore : this.selfScore;
        Level level = Level.fromScore(effectiveScore);
        this.level = level.name();
        this.updatedAt = OffsetDateTime.now();
    }

    public Score getEffectiveScore() {
        Short effective = this.verifiedScore != null ? this.verifiedScore : this.selfScore;
        return effective != null ? Score.of(effective) : Score.ZERO;
    }

    public Level getLevel() {
        return Level.fromScore(this.verifiedScore != null ? this.verifiedScore : this.selfScore);
    }
}