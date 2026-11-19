package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisibilityTest {

    @ParameterizedTest
    @ValueSource(strings = {"PRIVATE", "PENDING", "PUBLIC", "REJECTED", "RECRUITER"})
    void of_validVisibility_returnsInstance(String visibility) {
        Visibility v = Visibility.of(visibility);
        assertThat(v.value()).isEqualTo(visibility);
    }

    @Test
    void of_caseInsensitive_returnsInstance() {
        assertThat(Visibility.of("private").value()).isEqualTo("PRIVATE");
        assertThat(Visibility.of("Recruiter").value()).isEqualTo("RECRUITER");
    }

    @Test
    void of_invalid_throws() {
        assertThatThrownBy(() -> Visibility.of("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid visibility");
    }

    @Test
    void values_areCorrect() {
        assertThat(Visibility.PRIVATE.value()).isEqualTo("PRIVATE");
        assertThat(Visibility.PENDING.value()).isEqualTo("PENDING");
        assertThat(Visibility.PUBLIC.value()).isEqualTo("PUBLIC");
        assertThat(Visibility.REJECTED.value()).isEqualTo("REJECTED");
        assertThat(Visibility.RECRUITER.value()).isEqualTo("RECRUITER");
    }
}