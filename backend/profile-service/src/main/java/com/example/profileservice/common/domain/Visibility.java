package com.example.profileservice.common.domain;

import java.util.Arrays;
import java.util.Objects;

public enum Visibility {
    PRIVATE("PRIVATE"),
    PENDING("PENDING"),
    PUBLIC("PUBLIC"),
    REJECTED("REJECTED"),
    RECRUITER("RECRUITER");

    private final String value;

    Visibility(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Visibility of(String value) {
        return Arrays.stream(values())
            .filter(v -> v.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid visibility: " + value));
    }

    @Override
    public String toString() {
        return value;
    }
}