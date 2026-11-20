package com.example.apigateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpServletResponse;

final class GatewayError {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

    private GatewayError() {
    }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("""
                {"success":false,"statusCode":%d,"message":"%s","data":null,"timestamp":"%s"}
                """.formatted(status, message, LocalDateTime.now().format(ISO_FORMATTER)));
    }
}
