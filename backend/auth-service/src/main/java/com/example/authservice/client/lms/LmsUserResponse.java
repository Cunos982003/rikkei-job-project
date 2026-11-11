package com.example.authservice.client.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LmsUserResponse(
    String lmsUserId,
    String email,
    String fullName,
    String role,
    Object issuedAt
) {
    public Instant getIssuedAtInstant() {
        if (issuedAt == null) {
            return null;
        }
        if (issuedAt instanceof Number num) {
            long val = num.longValue();
            if (val > 100_000_000_000L) {
                return Instant.ofEpochMilli(val);
            }
            return Instant.ofEpochSecond(val);
        }
        String str = issuedAt.toString().trim();
        try {
            long epoch = Long.parseLong(str);
            if (epoch > 100_000_000_000L) {
                return Instant.ofEpochMilli(epoch);
            }
            return Instant.ofEpochSecond(epoch);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Instant.parse(str);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

