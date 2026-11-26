package com.example.recruitment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidEvaluationLinkException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidLink(InvalidEvaluationLinkException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("code", 410, "message", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", 401, "message", ex.getMessage()));
    }
}