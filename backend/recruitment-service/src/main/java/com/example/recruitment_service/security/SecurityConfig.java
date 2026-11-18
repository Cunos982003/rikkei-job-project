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
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/jobs/*/recruit",
                                "/api/v1/me/recruitments/*/withdraw",
                                "/api/v1/me/interviews/*/pick")
                        .hasRole("CANDIDATE")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/me/recruitments",
                                "/api/v1/me/recruitments/**",
                                "/api/v1/me/interviews")
                        .hasRole("CANDIDATE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/staff/recruitments")
                        .hasRole("STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/recruitments/*/cv-snapshot")
                        .hasAnyRole("CANDIDATE", "HR")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/recruitments/*",
                                "/api/v1/rounds/*/slots")
                        .hasAnyRole("HR", "STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/companies/me/interviews",
                                "/api/v1/hr/applications")
                        .hasRole("HR")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/recruitments/*/transition",
                                "/api/v1/recruitments/*/rounds",
                                "/api/v1/rounds/*/slots",
                                "/api/v1/rounds/*/reschedule")
                        .hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/recruitments/*/rounds/*")
                        .hasRole("HR")
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
