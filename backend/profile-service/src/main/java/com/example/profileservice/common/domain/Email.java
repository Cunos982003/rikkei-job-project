package com.example.profileservice.common.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Email {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return new Email(value);
    }

    public String value() {
        return value;
    }

    public String masked() {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) return "***";

        String local = value.substring(0, atIndex);
        String domain = value.substring(atIndex);

        if (local.length() <= 3) {
            return "***" + domain;
        }
        return local.substring(0, 3) + "***" + domain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
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