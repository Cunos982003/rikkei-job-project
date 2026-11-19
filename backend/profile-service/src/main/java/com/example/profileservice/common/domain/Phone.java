package com.example.profileservice.common.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Phone {
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$"
    );

    private final String value;

    private Phone(String value) {
        this.value = value;
    }

    public static Phone of(String value) {
        if (value == null || !PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone format");
        }
        return new Phone(value);
    }

    public String value() {
        return value;
    }

    public String masked() {
        if (value.length() < 4) return value;
        return value.substring(0, 3) + "***" + value.substring(value.length() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return Objects.equals(value, phone.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}