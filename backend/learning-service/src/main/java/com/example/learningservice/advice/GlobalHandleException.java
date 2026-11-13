package com.example.learningservice.advice;

import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.exception.ForbiddenException;
import com.example.learningservice.exception.ResourceNotFoundException;
import com.example.learningservice.exception.UnauthorizedException;
import com.example.learningservice.model.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalHandleException {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, List<Map<String, String>>>>> handleValidationErrors(
                        MethodArgumentNotValidException ex) {

                List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .map(err -> Map.of(
                                                "field", err.getField(),
                                                "message",
                                                err.getDefaultMessage() != null ? err.getDefaultMessage()
                                                                : "Dữ liệu không hợp lệ"))
                                .toList();

                log.warn("Dữ liệu đầu vào không hợp lệ: {}", fieldErrors);

                ApiResponse<Map<String, List<Map<String, String>>>> response = ApiResponse.error(
                                HttpStatus.BAD_REQUEST,
                                "Dữ liệu nhập vào không hợp lệ, vui lòng kiểm tra lại",
                                Map.of("errors", fieldErrors));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
                log.warn("Lỗi nghiệp vụ đào tạo: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
                log.warn("Chưa xác thực: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
                log.warn("Không có quyền truy cập: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage()));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
                log.warn("Không tìm thấy tài nguyên: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
                log.warn("Phương thức HTTP không được hỗ trợ: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED,
                                                "Phương thức " + ex.getMethod()
                                                                + " không được hỗ trợ cho đường dẫn này. Vui lòng kiểm tra lại phương thức hoặc đường dẫn API."));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
                log.warn("Không tìm thấy đường dẫn: {}", ex.getResourcePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(HttpStatus.NOT_FOUND,
                                                "Không tìm thấy tài nguyên với đường dẫn: " + ex.getResourcePath()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
                log.warn("Tham số không hợp lệ: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
                log.error("Lỗi không mong muốn trong learning-service: ", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau"));
        }
}
