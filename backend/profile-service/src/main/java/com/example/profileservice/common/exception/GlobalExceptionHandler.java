package com.example.profileservice.common.exception;

import com.example.profileservice.client.LmsClient;
import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.service.InvalidTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, String>>>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Không hợp lệ"
                ))
                .toList();

        ApiResponse<Map<String, List<Map<String, String>>>> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu nhập vào không hợp lệ, vui lòng kiểm tra lại",
                Map.of("errors", fieldErrors)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(LmsClient.LmsClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleLmsClientException(LmsClient.LmsClientException ex) {
        HttpStatus status = ex.getStatusCode() != null ? ex.getStatusCode() : HttpStatus.SERVICE_UNAVAILABLE;
        HttpStatus responseStatus = (status == HttpStatus.NOT_FOUND) ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(responseStatus)
                .body(ApiResponse.error(responseStatus, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception in profile-service: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra sự cố trên hệ thống máy chủ: " + ex.getMessage()));
    }
}