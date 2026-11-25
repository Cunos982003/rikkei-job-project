package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.ApiResponse;
import com.example.ai_career_service.dto.chat.AiQuotaResponse;
import com.example.ai_career_service.service.QuotaExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiResponse<AiQuotaResponse>> handleQuotaExceeded(QuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), ex.getQuota()));
    }
}
