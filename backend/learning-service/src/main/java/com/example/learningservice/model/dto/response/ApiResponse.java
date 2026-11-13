package com.example.learningservice.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        boolean success,
        int statusCode,
        String message,
        T data,
        String timestamp
) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                HttpStatus.OK.value(),
                message,
                data,
                LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(
                true,
                HttpStatus.CREATED.value(),
                message,
                data,
                LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(
                false,
                status.value(),
                message,
                data,
                LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}

