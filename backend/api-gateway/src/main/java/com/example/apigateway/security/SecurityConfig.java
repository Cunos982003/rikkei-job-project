package com.example.apigateway.security;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain gatewaySecurity(HttpSecurity http, StringRedisTemplate redis,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.gateway.rate-limit-per-minute}") int limit) throws Exception {
        JwtAuthenticationFilter jwt = new JwtAuthenticationFilter(secret);
        RateLimitFilter.IpRateLimitFilter ipRateLimit = new RateLimitFilter.IpRateLimitFilter(redis, limit);
        RateLimitFilter.UserRateLimitFilter userRateLimit = new RateLimitFilter.UserRateLimitFilter(redis, limit);
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, ex) -> GatewayError.write(response, 401, "Yêu cầu xác thực"))
                        .accessDeniedHandler(
                                (request, response, ex) -> GatewayError.write(response, 403, "Truy cập bị từ chối")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/profiles/me/internships",
                                "/api/services/profile/api/v1/profiles/me/internships").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/sso", "/api/v1/auth/login",
                                "/api/v1/auth/hr/login", "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/auth/sso", "/auth/login", "/auth/hr/login",
                                "/auth/forgot-password", "/auth/reset-password",
                                "/api/services/auth/api/v1/auth/sso", "/api/services/auth/api/v1/auth/login",
                                "/api/services/auth/api/v1/auth/hr/login",
                                "/api/services/auth/api/v1/auth/forgot-password",
                                "/api/services/auth/api/v1/auth/reset-password",
                                "/api/services/auth/auth/sso", "/api/services/auth/auth/login",
                                "/api/services/auth/auth/hr/login", "/api/services/auth/auth/forgot-password",
                                "/api/services/auth/auth/reset-password",
                                "/api/services/job/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(ipRateLimit, JwtAuthenticationFilter.class)
                .addFilterAfter(userRateLimit, JwtAuthenticationFilter.class)
                .addFilterAfter(new IdentityHeaderFilter(), RateLimitFilter.UserRateLimitFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.gateway.allowed-origins}") String origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-User-Id"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
