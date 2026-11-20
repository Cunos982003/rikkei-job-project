package com.example.profileservice.common.domain;

import java.util.Objects;

public final class Score {
    public static final Score ZERO = new Score((short) 0);
    private final short value;

    private Score(short value) {
        this.value = value;
    }

    public static Score of(short value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
        return new Score(value);
    }

    public short value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score = (Score) o;
        return value == score.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}