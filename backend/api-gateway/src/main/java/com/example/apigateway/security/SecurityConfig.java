package com.example.apigateway.security;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain gatewaySecurity(HttpSecurity http, StringRedisTemplate redis,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.gateway.rate-limit-per-minute}") int limit,
            @Value("${app.gateway.rate-limit-fail-open:true}") boolean rateLimitFailOpen) throws Exception {
        JwtAuthenticationFilter jwt = new JwtAuthenticationFilter(secret);
        RateLimitFilter.IpRateLimitFilter ipRateLimit = new RateLimitFilter.IpRateLimitFilter(redis, limit, rateLimitFailOpen);
        RateLimitFilter.UserRateLimitFilter userRateLimit = new RateLimitFilter.UserRateLimitFilter(redis, limit, rateLimitFailOpen);
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, ex) -> GatewayError.write(response, 401, "Yêu cầu xác thực tài khoản"))
                        .accessDeniedHandler(
                                (request, response, ex) -> GatewayError.write(response, 403, "Bạn không có quyền truy cập tài nguyên này")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/actuator/**",
                                "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/profiles/me/internships",
                                "/api/services/profile/api/v1/profiles/me/internships").denyAll()
                        // Public Auth APIs
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
                        // Public GET APIs cho khách chưa đăng nhập tra cứu thông tin
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/jobs", "/api/v1/jobs/**", "/jobs", "/jobs/**",
                                "/api/v1/skills", "/api/v1/skills/**",
                                "/api/v1/candidates", "/api/v1/candidates/**",
                                "/api/v1/candidate-directory/**",
                                "/api/v1/learning/target-positions", "/api/v1/learning/target-positions/**",
                                "/api/v1/files/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
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
        cors.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept",
                "X-User-Id", "X-User-Role", "X-PM-Student-Id",
                "X-Service-Key", "X-Internal-Service-Token",
                "X-Correlation-Id", "x-correlation-id", "X-Request-Id", "traceparent"
        ));
        cors.setExposedHeaders(List.of(
                "X-Correlation-Id", "traceparent"
        ));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
