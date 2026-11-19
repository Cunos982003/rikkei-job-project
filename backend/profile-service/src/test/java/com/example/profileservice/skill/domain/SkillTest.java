package com.example.profileservice.skill.domain;

import com.example.profileservice.common.domain.SkillId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkillTest {

    @Test
    void getSkillId_returnsSkillIdWrapper() {
        UUID uuid = UUID.randomUUID();
        Skill skill = Skill.builder()
            .id(uuid)
            .code("JAVA_CORE")
            .name("Java Core")
            .group("Programming")
            .build();

        SkillId skillId = skill.getSkillId();
        assertThat(skillId.value()).isEqualTo(uuid);
    }
}