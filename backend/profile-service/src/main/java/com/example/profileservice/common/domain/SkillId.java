package com.example.profileservice.common.domain;

import java.util.Objects;
import java.util.UUID;

public final class SkillId {
    private final UUID value;

    private SkillId(UUID value) {
        this.value = value;
    }

    public static SkillId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("SkillId must not be null");
        }
        return new SkillId(value);
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillId skillId = (SkillId) o;
        return Objects.equals(value, skillId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}