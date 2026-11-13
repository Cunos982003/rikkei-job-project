package com.example.authservice.security;

import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public static final String DEFAULT_ROLE = "ROLE_CANDIDATE";

    private static final Set<String> VALID_ROLES = Set.of(
        "CANDIDATE",
        "HR",
        "MENTOR",
        "STAFF",
        "ADMIN"
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("STUDENT", "CANDIDATE"),
        Map.entry("ROLE_STUDENT", "CANDIDATE"),
        Map.entry("CANDIDATE", "CANDIDATE"),
        Map.entry("ROLE_CANDIDATE", "CANDIDATE"),
        Map.entry("HR", "HR"),
        Map.entry("ROLE_HR", "HR"),
        Map.entry("MENTOR", "MENTOR"),
        Map.entry("ROLE_MENTOR", "MENTOR"),
        Map.entry("STAFF", "STAFF"),
        Map.entry("ROLE_STAFF", "STAFF"),
        Map.entry("ADMIN", "ADMIN"),
        Map.entry("ROLE_ADMIN", "ADMIN")
    );

    public String mapAndValidateLmsRole(String rawRole) {
        String normalized = normalizeRoleName(rawRole);
        if (normalized == null) {
            throw new SsoException(ErrorCode.INVALID_ROLE, "Role cannot be null or empty");
        }

        if (!VALID_ROLES.contains(normalized)) {
            throw new SsoException(ErrorCode.INVALID_ROLE, "Role '" + rawRole + "' is not a valid role in the shared authorization framework");
        }

        return normalized;
    }

    public static String normalizeToSecurityRole(String rawRole) {
        String normalized = normalizeRoleName(rawRole);
        if (normalized == null) {
            return DEFAULT_ROLE;
        }
        if (!VALID_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid role '" + rawRole + "'. Supported roles: CANDIDATE, HR, MENTOR, STAFF, ADMIN");
        }
        return "ROLE_" + normalized;
    }

    public static boolean isSupportedRole(String rawRole) {
        String normalized = normalizeRoleName(rawRole);
        return normalized != null && VALID_ROLES.contains(normalized);
    }

    public static String normalizeToAuthority(String rawRole) {
        return normalizeToSecurityRole(rawRole);
    }

    private static String normalizeRoleName(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }

        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        String aliased = ALIASES.get(normalized);
        if (aliased != null) {
            return aliased;
        }

        return normalized;
    }
}

