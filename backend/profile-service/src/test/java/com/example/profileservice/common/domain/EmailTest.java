package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void of_validEmail_returnsInstance() {
        Email e = Email.of("user@example.com");
        assertThat(e.value()).isEqualTo("user@example.com");
    }

    @Test
    void of_invalidFormat_throws() {
        assertThatThrownBy(() -> Email.of("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void masked_shortLocalPart() {
        assertThat(Email.of("ab@example.com").masked()).isEqualTo("***@example.com");
    }

    @Test
    void masked_longLocalPart() {
        assertThat(Email.of("username@example.com").masked()).isEqualTo("use***@example.com");
    }

    @Test
    void masked_threeCharLocalPart() {
        assertThat(Email.of("abc@example.com").masked()).isEqualTo("***@example.com");
    }

    @Test
    void equals_and_hashCode_work() {
        Email e1 = Email.of("test@example.com");
        Email e2 = Email.of("test@example.com");
        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }
}