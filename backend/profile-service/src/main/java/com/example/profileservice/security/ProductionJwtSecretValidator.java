package com.example.profileservice.security;

import java.util.Arrays;
import java.util.Set;
import org.springframework.core.env.Environment;

public final class ProductionJwtSecretValidator {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");
    private static final String FALLBACK_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private ProductionJwtSecretValidator() {
    }

    public static String requireValidSecret(Environment environment) {
        boolean isProductionProfile = isProductionProfile(environment);

        if (!isProductionProfile) {
            return resolveLocalSecret(environment);
        }

        String secret = readIfPresent(
            environment.getProperty("JWT_SECRET"),
            System.getProperty("JWT_SECRET"),
            System.getenv("JWT_SECRET")
        );
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Production JWT secret is required. Set JWT_SECRET via environment configuration.");
        }

        String normalizedSecret = secret.trim();
        if (normalizedSecret.length() < 32 || FALLBACK_SECRET.equals(normalizedSecret)) {
            throw new IllegalStateException("Production JWT secret is invalid or uses the fallback value. Set a secure JWT_SECRET from environment configuration.");
        }

        return normalizedSecret;
    }

    private static String resolveLocalSecret(Environment environment) {
        String secret = readIfPresent(
            environment.getProperty("JWT_SECRET"),
            System.getProperty("JWT_SECRET"),
            System.getenv("JWT_SECRET"),
            environment.getProperty("app.jwt.secret")
        );
        return secret == null ? null : secret.trim();
    }

    private static boolean isProductionProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
            .anyMatch(PRODUCTION_PROFILES::contains)
            || PRODUCTION_PROFILES.contains(environment.getProperty("spring.profiles.active", "").trim().toLowerCase());
    }

    private static String readIfPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
