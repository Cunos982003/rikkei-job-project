package com.example.profileservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String TEST_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existingAuthenticatedContextIsNotOverwrittenByValidJwt() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(TEST_JWT_SECRET);
        String existingPrincipal = "existing-authentication";
        Authentication existingAuthentication = new UsernamePasswordAuthenticationToken(
                existingPrincipal,
                "existing-password",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        UUID userId = UUID.randomUUID();
        String jwt = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("role", "ROLE_HR")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> { };

        filter.doFilter(request, response, filterChain);

        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(actualAuthentication).isNotNull();
        assertThat(actualAuthentication.getName()).isEqualTo(existingPrincipal);
        assertThat(actualAuthentication.getAuthorities()).extracting("authority").doesNotContain("ROLE_HR");
    }

    @Test
    void validJwtCreatesAuthenticationInEmptyContext() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(TEST_JWT_SECRET);
        UUID userId = UUID.randomUUID();
        String jwt = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("role", "CANDIDATE")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> { };

        filter.doFilter(request, response, filterChain);

        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(actualAuthentication).isNotNull();
        assertThat(actualAuthentication.getName()).isEqualTo(userId.toString());
        assertThat(actualAuthentication.getAuthorities()).extracting("authority").containsExactly("ROLE_CANDIDATE");
    }

    @Test
    void invalidJwtDoesNotCreateAuthentication() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(TEST_JWT_SECRET);
        String invalidJwt = "invalid.jwt.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidJwt);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> { };

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingJwtLeavesSecurityContextEmpty() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(TEST_JWT_SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> { };

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void wrongRoleDoesNotCreateValidAuthority() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(TEST_JWT_SECRET);
        UUID userId = UUID.randomUUID();
        String jwt = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("role", "ROLE_INVALID")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> { };

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
