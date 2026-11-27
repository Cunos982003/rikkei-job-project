package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

    @Test
    void of_validScore_returnsInstance() {
        Score s = Score.of((short) 85);
        assertThat(s.value()).isEqualTo((short) 85);
    }

    @Test
    void of_minBoundary_returnsZero() {
        assertThat(Score.of((short) 0).value()).isEqualTo((short) 0);
    }

    @Test
    void of_maxBoundary_returnsHundred() {
        assertThat(Score.of((short) 100).value()).isEqualTo((short) 100);
    }

    @Test
    void of_belowZero_throws() {
        assertThatThrownBy(() -> Score.of((short) -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Score must be between 0 and 100");
    }

    @Test
    void of_aboveHundred_throws() {
        assertThatThrownBy(() -> Score.of((short) 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Score must be between 0 and 100");
    }

    @Test
    void zero_constant() {
        assertThat(Score.ZERO.value()).isEqualTo((short) 0);
    }

    @Test
    void equals_and_hashCode_work() {
        Score s1 = Score.of((short) 50);
        Score s2 = Score.of((short) 50);
        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    }
}