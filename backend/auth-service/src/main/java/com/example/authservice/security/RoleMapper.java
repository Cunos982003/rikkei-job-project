package com.example.authservice.security;

import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    private static final Set<String> VALID_LMS_ROLES = Set.of(
        "STUDENT",
        "CANDIDATE",
        "MENTOR",
        "STAFF",
        "ADMIN"
    );

    public String mapAndValidateLmsRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new SsoException(ErrorCode.INVALID_ROLE, "Role cannot be null or empty");
        }

        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        if (!VALID_LMS_ROLES.contains(normalized)) {
            throw new SsoException(ErrorCode.INVALID_ROLE, "Role '" + rawRole + "' is not a valid LMS role in NT-01");
        }

        if ("STUDENT".equals(normalized) || "CANDIDATE".equals(normalized)) {
            return "CANDIDATE";
        }

        return normalized;
    }
}

