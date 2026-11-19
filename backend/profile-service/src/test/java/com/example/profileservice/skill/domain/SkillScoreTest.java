package com.example.profileservice.skill.domain;

import com.example.profileservice.common.domain.Level;
import com.example.profileservice.common.domain.Score;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillScoreTest {

    @Test
    void recalculate_withVerifiedEvidences_setsVerifiedScoreAndLevel() {
        SkillScore s = SkillScore.builder().build();
        List<SkillEvidence> evidences = List.of(
            SkillEvidence.builder().score((short) 80).verified(true).recordedAt(OffsetDateTime.now().minusDays(10)).build(),
            SkillEvidence.builder().score((short) 90).verified(true).recordedAt(OffsetDateTime.now().minusDays(20)).build()
        );
        s.recalculate(evidences);
        assertThat(s.getVerifiedScore()).isEqualTo((short) 85); // avg of 80 and 90
        assertThat(s.getLevel()).isEqualTo(Level.VUNG); // 85 -> VUNG
    }

    @Test
    void recalculate_withSelfOnly_setsSelfScoreAndLevel() {
        SkillScore s = SkillScore.builder().build();
        List<SkillEvidence> evidences = List.of(
            SkillEvidence.builder().score((short) 90).verified(false).build()
        );
        s.recalculate(evidences);
        assertThat(s.getSelfScore()).isEqualTo((short) 90);
        assertThat(s.getVerifiedScore()).isNull();
        assertThat(s.getLevel()).isEqualTo(Level.NANG_CAO); // self 90 -> NANG_CAO
    }

    @Test
    void recalculate_withMixed_usesVerifiedForLevel() {
        SkillScore s = SkillScore.builder().build();
        List<SkillEvidence> evidences = List.of(
            SkillEvidence.builder().score((short) 60).verified(true).build(),  // verified 60 -> KHA
            SkillEvidence.builder().score((short) 90).verified(false).build() // self 90
        );
        s.recalculate(evidences);
        assertThat(s.getVerifiedScore()).isEqualTo((short) 60);
        assertThat(s.getSelfScore()).isEqualTo((short) 90);
        assertThat(s.getLevel()).isEqualTo(Level.KHA); // effective = verified (60)
    }

    @Test
    void recalculate_withDecay_olderEvidence_halved() {
        SkillScore s = SkillScore.builder().build();
        List<SkillEvidence> evidences = List.of(
            SkillEvidence.builder().score((short) 100).verified(true).recordedAt(OffsetDateTime.now().minusDays(366)).build() // decayed to 50
        );
        s.recalculate(evidences);
        assertThat(s.getVerifiedScore()).isEqualTo((short) 50);
        assertThat(s.getLevel()).isEqualTo(Level.KHA); // 50 -> KHA
    }

    @Test
    void getEffectiveScore_verifiedPresent_returnsVerified() {
        SkillScore s = SkillScore.builder()
            .verifiedScore((short) 70)
            .selfScore((short) 90)
            .build();
        assertThat(s.getEffectiveScore().value()).isEqualTo((short) 70);
    }

    @Test
    void getEffectiveScore_verifiedNull_returnsSelf() {
        SkillScore s = SkillScore.builder()
            .verifiedScore(null)
            .selfScore((short) 80)
            .build();
        assertThat(s.getEffectiveScore().value()).isEqualTo((short) 80);
    }

    @Test
    void getEffectiveScore_bothNull_returnsZero() {
        SkillScore s = SkillScore.builder()
            .verifiedScore(null)
            .selfScore(null)
            .build();
        assertThat(s.getEffectiveScore()).isEqualTo(Score.ZERO);
    }

    @Test
    void getLevel_usesEffectiveScore() {
        SkillScore s = SkillScore.builder()
            .verifiedScore((short) 60)
            .selfScore((short) 90)
            .build();
        assertThat(s.getLevel()).isEqualTo(Level.KHA); // effective = verified (60)
    }
}