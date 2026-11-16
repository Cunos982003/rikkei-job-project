package com.example.apigateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

abstract class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], 60) end
            if count > tonumber(ARGV[1]) then return 0 end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int limit;

    RateLimitFilter(StringRedisTemplate redis, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Rate limit must be at least one request per minute");
        }
        this.redis = redis;
        this.limit = limit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String identity = identity(request);
        if (identity == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long allowed = redis.execute(SCRIPT, List.of("gateway:rate:" + identity), Integer.toString(limit));
            if (allowed == null) {
                log.error("Redis returned no result for the gateway rate limit");
                GatewayError.write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Dịch vụ giới hạn tần suất không khả dụng");
                return;
            }
            if (allowed != 1L) {
                GatewayError.write(response, 429, "Bạn đã gửi quá nhiều yêu cầu, vui lòng thử lại sau");
                return;
            }
        } catch (DataAccessException ex) {
            log.error("Gateway rate limiting is unavailable", ex);
            GatewayError.write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Dịch vụ giới hạn tần suất không khả dụng");
            return;
        }
        chain.doFilter(request, response);
    }

    protected abstract String identity(HttpServletRequest request);

    protected static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    static final class IpRateLimitFilter extends RateLimitFilter {
        IpRateLimitFilter(StringRedisTemplate redis, int limit) {
            super(redis, limit);
        }

        @Override
        protected String identity(HttpServletRequest request) {
            return "ip:" + digest(request.getRemoteAddr());
        }
    }

    static final class UserRateLimitFilter extends RateLimitFilter {
        UserRateLimitFilter(StringRedisTemplate redis, int limit) {
            super(redis, limit);
        }

        @Override
        protected String identity(HttpServletRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated()
                    ? "user:" + digest(authentication.getName()) : null;
        }
    }
}
