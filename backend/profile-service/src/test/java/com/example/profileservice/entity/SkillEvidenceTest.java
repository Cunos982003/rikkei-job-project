package com.example.profileservice.entity;

import com.example.profileservice.common.domain.Score;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkillEvidenceTest {

    @Test
    void updateScore_updatesScoreAndRecordedAt() {
        SkillEvidence e = SkillEvidence.builder()
            .score((short) 50)
            .verified(true)
            .recordedAt(OffsetDateTime.now().minusDays(10))
            .build();
        OffsetDateTime before = e.getRecordedAt();
        e.updateScore(Score.of((short) 80));
        assertThat(e.getScore()).isEqualTo((short) 80);
        assertThat(e.getRecordedAt()).isAfter(before);
    }

    @Test
    void getDecayedScore_withinYear_returnsFullScore() {
        SkillEvidence e = SkillEvidence.builder()
            .score((short) 80)
            .verified(true)
            .recordedAt(OffsetDateTime.now().minusDays(30))
            .build();
        assertThat(e.getDecayedScore()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void getDecayedScore_olderThanYear_returnsHalfScore() {
        SkillEvidence e = SkillEvidence.builder()
            .score((short) 80)
            .verified(true)
            .recordedAt(OffsetDateTime.now().minusDays(366))
            .build();
        assertThat(e.getDecayedScore()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    void isRecent_withinYear_true() {
        SkillEvidence e = SkillEvidence.builder()
            .recordedAt(OffsetDateTime.now().minusDays(30))
            .build();
        assertThat(e.isRecent()).isTrue();
    }

    @Test
    void isRecent_olderThanYear_false() {
        SkillEvidence e = SkillEvidence.builder()
            .recordedAt(OffsetDateTime.now().minusDays(366))
            .build();
        assertThat(e.isRecent()).isFalse();
    }

    @Test
    void getDecayedScore_nullScore_returnsZero() {
        SkillEvidence e = SkillEvidence.builder()
            .score(null)
            .build();
        assertThat(e.getDecayedScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}