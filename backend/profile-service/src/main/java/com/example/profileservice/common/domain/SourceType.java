package com.example.profileservice.common.domain;

import java.util.Arrays;
import java.util.Objects;

public enum SourceType {
    LMS("LMS"),
    EXAM("EXAM"),
    INTERNSHIP("INTERNSHIP"),
    MENTOR("MENTOR"),
    SELF("SELF");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SourceType of(String value) {
        return Arrays.stream(values())
            .filter(v -> v.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid source type: " + value));
    }

    @Override
    public String toString() {
        return value;
    }
}