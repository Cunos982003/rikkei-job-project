package vn.rikkei.jobs.assessment.security;

import java.util.UUID;

public record MentorPrincipal(UUID userId, String email, String role) {

    public boolean isMentor() {
        return "MENTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }
}
