package com.example.recruitment_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/jobs/*/recruit",
                                "/api/v1/jobs/*/apply",
                                "/api/v1/me/recruitments/*/withdraw",
                                "/api/v1/me/interviews/*/pick",
                                "/api/v1/me/offers/*/accept",
                                "/api/v1/me/offers/*/decline")
                        .hasRole("CANDIDATE")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/me/recruitments",
                                "/api/v1/me/recruitments/**",
                                "/api/v1/me/interviews",
                                "/api/v1/me/offers",
                                "/api/v1/me/offers/**",
                                "/api/v1/me/employment-records")
                        .hasRole("CANDIDATE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/staff/recruitments", "/api/v1/staff/applications")
                        .hasRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/recruitments/*/cv-snapshot")
                        .hasAnyRole("CANDIDATE", "HR")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/recruitments/*",
                                "/api/v1/applications/*",
                                "/api/v1/rounds/*/slots")
                        .hasAnyRole("HR", "STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/companies/me/interviews",
                                "/api/v1/hr/applications",
                                "/api/v1/applications")
                        .hasRole("HR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/recruitments/*/transition",
                                "/api/v1/recruitments/*/rounds",
                                "/api/v1/applications/*/transition",
                                "/api/v1/applications/*/rounds",
                                "/api/v1/applications/*/offer",
                                "/api/v1/recruitments/*/offer",
                                "/api/v1/applications/*/offer/confirm-onboard",
                                "/api/v1/recruitments/*/offer/confirm-onboard",
                                "/api/v1/rounds/*/slots",
                                "/api/v1/rounds/*/reschedule")
                        .hasRole("HR")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/recruitments/*/rounds/*",
                                "/api/v1/applications/*/rounds/*")
                        .hasRole("HR")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/recruitments/*/rounds/*",
                                "/api/v1/applications/*/rounds/*")
                        .hasRole("HR")
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
