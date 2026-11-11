package com.example.authservice.dto;

import java.time.Instant;

public record ErrorResponse(
    int status,
    String code,
    String message,
    String timestamp
) {
    public ErrorResponse(int status, String code, String message) {
        this(status, code, message, Instant.now().toString());
    }
}
