package com.example.ai_career_service.utils;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AuthenticatedCandidate {

    private AuthenticatedCandidate() {
    }

    public static UUID requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated candidate is required");
        }
        try {
            UUID userId = UUID.fromString(principal.getName());
            if (!userId.toString().equalsIgnoreCase(principal.getName())) {
                throw new IllegalArgumentException("Invalid UUID format");
            }
            return userId;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated candidate ID must be a UUID",
                    exception);
        }
    }
}
