package com.example.profileservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.profileservice.security.ProductionJwtSecretValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionJwtSecretValidatorTest {

    private static final String FALLBACK_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Test
    @DisplayName("Production requires an external JWT_SECRET")
    void productionRequiresExternalSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("app.jwt.secret", FALLBACK_SECRET);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ProductionJwtSecretValidator.requireValidSecret(environment));

        assertEquals("Production JWT secret is required. Set JWT_SECRET via environment configuration.", ex.getMessage());
    }

    @Test
    @DisplayName("Production rejects blank JWT_SECRET")
    void productionRejectsBlankSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("JWT_SECRET", "   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ProductionJwtSecretValidator.requireValidSecret(environment));

        assertEquals("Production JWT secret is required. Set JWT_SECRET via environment configuration.", ex.getMessage());
    }

    @Test
    @DisplayName("Production rejects fallback secret value")
    void productionRejectsFallbackSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("JWT_SECRET", FALLBACK_SECRET);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ProductionJwtSecretValidator.requireValidSecret(environment));

        assertEquals("Production JWT secret is invalid or uses the fallback value. Set a secure JWT_SECRET from environment configuration.", ex.getMessage());
    }

    @Test
    @DisplayName("Production accepts a valid external secret")
    void productionAcceptsValidSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("JWT_SECRET", "a-very-long-production-secret-value-1234567890");

        String secret = ProductionJwtSecretValidator.requireValidSecret(environment);

        assertEquals("a-very-long-production-secret-value-1234567890", secret);
    }

    @Test
    @DisplayName("Local profile continues to allow existing fallback configuration")
    void localProfileKeepsExistingFallback() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.jwt.secret", FALLBACK_SECRET);

        String secret = ProductionJwtSecretValidator.requireValidSecret(environment);

        assertEquals(FALLBACK_SECRET, secret);
    }
}
