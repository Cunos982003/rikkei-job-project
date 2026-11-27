package com.example.apigateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpServletResponse;

public record GatewayError(
        boolean success,
        int statusCode,
        String message,
        Object data,
        String timestamp
) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

    public static GatewayError of(int statusCode, String message) {
        return new GatewayError(
                false,
                statusCode,
                message,
                null,
                LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        GatewayError error = GatewayError.of(status, message);
        response.getWriter().write(error.toJson());
    }

    public String toJson() {
        return """
                {"success":false,"statusCode":%d,"message":"%s","data":null,"timestamp":"%s"}
                """.formatted(statusCode, escapeJson(message), timestamp);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
