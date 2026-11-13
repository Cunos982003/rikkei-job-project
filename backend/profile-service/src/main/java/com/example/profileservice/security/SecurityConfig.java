package com.example.profileservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/files").hasRole("CANDIDATE")
                .requestMatchers(HttpMethod.GET, "/api/v1/files/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me/educations/**").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/profiles/me/educations").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me/educations/**").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/profiles/me/educations/**").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me/certificates/**").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/profiles/me/certificates").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me/certificates/**").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/profiles/me/certificates/**").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me/skills").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/{userId}/skills").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me/internships").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/profiles/{userId}/internships").hasAnyRole("CANDIDATE", "HR", "MENTOR", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me/self-skills").hasRole("CANDIDATE")
                .requestMatchers("/api/v1/profiles/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
