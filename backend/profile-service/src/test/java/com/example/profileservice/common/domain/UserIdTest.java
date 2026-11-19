package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void of_validUuid_returnsInstance() {
        UUID uuid = UUID.randomUUID();
        UserId id = UserId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void of_null_throws() {
        assertThatThrownBy(() -> UserId.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("UserId must not be null");
    }

    @Test
    void equals_and_hashCode_work() {
        UUID uuid = UUID.randomUUID();
        assertThat(UserId.of(uuid)).isEqualTo(UserId.of(uuid));
        assertThat(UserId.of(uuid).hashCode()).isEqualTo(UserId.of(uuid).hashCode());
    }
}