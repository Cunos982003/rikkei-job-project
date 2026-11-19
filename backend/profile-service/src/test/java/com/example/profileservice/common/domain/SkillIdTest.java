package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillIdTest {

    @Test
    void of_validUuid_returnsInstance() {
        UUID uuid = UUID.randomUUID();
        SkillId id = SkillId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void of_null_throws() {
        assertThatThrownBy(() -> SkillId.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SkillId must not be null");
    }

    @Test
    void equals_and_hashCode_work() {
        UUID uuid = UUID.randomUUID();
        assertThat(SkillId.of(uuid)).isEqualTo(SkillId.of(uuid));
        assertThat(SkillId.of(uuid).hashCode()).isEqualTo(SkillId.of(uuid).hashCode());
    }
}