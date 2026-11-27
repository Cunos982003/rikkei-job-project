package com.example.authservice.metrics;

public enum AuthResult {
    SUCCESS("success"),
    FAILURE("failure");

    private final String value;

    AuthResult(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
