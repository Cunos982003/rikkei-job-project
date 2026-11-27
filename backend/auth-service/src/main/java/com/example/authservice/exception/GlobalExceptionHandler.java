package com.example.authservice.exception;

import com.example.authservice.dto.ApiResponse;
import com.example.authservice.metrics.AuthEndpoint;
import com.example.authservice.metrics.AuthMetricsService;
import com.example.authservice.metrics.AuthReason;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuthMetricsService authMetricsService;

    public GlobalExceptionHandler(AuthMetricsService authMetricsService) {
        this.authMetricsService = authMetricsService;
    }

    private void recordAuthMetricIfApplicable(HttpServletRequest request, AuthReason reason) {
        if (request == null) {
            return;
        }
        AuthEndpoint endpoint = AuthEndpoint.fromUri(request.getRequestURI());
        if (endpoint != null) {
            authMetricsService.recordFailure(endpoint, reason);
        }
    }

    @ExceptionHandler(SsoException.class)
    public ResponseEntity<ApiResponse<Void>> handleSsoException(SsoException ex, HttpServletRequest request) {
        log.warn("SSO error [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        recordAuthMetricIfApplicable(request, AuthReason.fromErrorCode(ex.getErrorCode()));
        ErrorCode errorCode = ex.getErrorCode();
        String message = resolveVietnameseMessage(errorCode, ex.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getHttpStatus(), errorCode.getCode(), message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());
        recordAuthMetricIfApplicable(request, AuthReason.INVALID_CREDENTIALS);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex, HttpServletRequest request) {
        log.warn("Account locked: {}", ex.getMessage());
        recordAuthMetricIfApplicable(request, AuthReason.ACCOUNT_LOCKED);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền truy cập tài nguyên này"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Phiên đăng nhập đã hết hạn hoặc bạn chưa được xác thực"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        recordAuthMetricIfApplicable(request, AuthReason.VALIDATION_ERROR);
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Không hợp lệ"
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "Dữ liệu nhập vào không hợp lệ, vui lòng kiểm tra lại",
                        Map.of("errors", fieldErrors)
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Database constraint violation", ex);
        recordAuthMetricIfApplicable(request, AuthReason.TICKET_ALREADY_USED);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ErrorCode.TICKET_ALREADY_USED.getCode(), "Dữ liệu đã tồn tại trong hệ thống hoặc vé đã được sử dụng"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Response status exception: {}", ex.getReason());
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status, "STATUS_ERROR", ex.getReason() != null ? ex.getReason() : "Yêu cầu không thể xử lý"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy tài nguyên: /" + ex.getResourcePath()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage() != null ? ex.getMessage() : "Dữ liệu yêu cầu không hợp lệ"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error", ex);
        recordAuthMetricIfApplicable(request, AuthReason.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getCode(), "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau"));
    }

    private String resolveVietnameseMessage(ErrorCode code, String rawMessage) {
        if (rawMessage != null) {
            if (rawMessage.contains("Tài khoản") || rawMessage.contains("khóa") || rawMessage.contains("khoá") || rawMessage.contains("hợp lệ")) {
                return rawMessage;
            }
        }
        return switch (code) {
            case TICKET_INVALID -> "Vé SSO không hợp lệ hoặc không tồn tại";
            case TICKET_EXPIRED -> "Vé SSO đã hết hạn (chỉ có hiệu lực trong 60 giây)";
            case TICKET_ALREADY_USED -> "Vé SSO đã được sử dụng";
            case INVALID_ROLE -> "Vai trò người dùng không hợp lệ hoặc chưa được cấp quyền";
            case INVALID_CREDENTIALS -> {
                if (rawMessage != null && rawMessage.toLowerCase().contains("current password")) {
                    yield "Mật khẩu hiện tại không chính xác";
                }
                if (rawMessage != null && rawMessage.toLowerCase().contains("at least 8")) {
                    yield "Mật khẩu phải có độ dài tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số";
                }
                yield "Email hoặc mật khẩu không chính xác";
            }
            case ACCOUNT_LOCKED -> "Tài khoản của bạn đã bị tạm khóa do nhập sai mật khẩu nhiều lần";
            case RESET_TOKEN_INVALID -> "Mã đặt lại mật khẩu không hợp lệ";
            case RESET_TOKEN_EXPIRED -> "Mã đặt lại mật khẩu đã hết hạn (có hiệu lực trong 30 phút)";
            case RESET_TOKEN_ALREADY_USED -> "Mã đặt lại mật khẩu đã được sử dụng hoặc đã hết hạn";
            case MUST_CHANGE_PASSWORD -> "Bắt buộc phải đổi mật khẩu lần đầu trước khi tiếp tục truy cập";
            case EXTERNAL_UNAVAILABLE -> "Hệ thống LMS tạm thời không khả dụng. Vui lòng thử lại sau";
            case LMS_ERROR -> "Hệ thống LMS phản hồi lỗi trong quá trình xác thực";
            case LMS_UNAUTHORIZED -> "Hệ thống LMS từ chối quyền truy cập (HTTP 401)";
            case DATABASE_ERROR -> "Đã xảy ra sự cố cơ sở dữ liệu";
            case INTERNAL_ERROR -> "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau";
        };
    }
}

