package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"LMS", "EXAM", "INTERNSHIP", "MENTOR", "SELF"})
    void of_validSource_returnsInstance(String source) {
        SourceType st = SourceType.of(source);
        assertThat(st.value()).isEqualTo(source);
    }

    @Test
    void of_caseInsensitive_returnsInstance() {
        assertThat(SourceType.of("lms").value()).isEqualTo("LMS");
        assertThat(SourceType.of("Self").value()).isEqualTo("SELF");
    }

    @Test
    void of_invalid_throws() {
        assertThatThrownBy(() -> SourceType.of("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid source type");
    }

    @Test
    void values_areCorrect() {
        assertThat(SourceType.LMS.value()).isEqualTo("LMS");
        assertThat(SourceType.EXAM.value()).isEqualTo("EXAM");
        assertThat(SourceType.INTERNSHIP.value()).isEqualTo("INTERNSHIP");
        assertThat(SourceType.MENTOR.value()).isEqualTo("MENTOR");
        assertThat(SourceType.SELF.value()).isEqualTo("SELF");
    }
}