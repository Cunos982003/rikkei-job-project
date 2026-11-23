package com.example.authservice.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    TICKET_INVALID(HttpStatus.BAD_REQUEST, "TICKET_INVALID", "Vé SSO không hợp lệ"),
    TICKET_EXPIRED(HttpStatus.UNAUTHORIZED, "TICKET_EXPIRED", "Vé SSO đã hết hạn (chỉ có hiệu lực trong 60 giây)"),
    TICKET_ALREADY_USED(HttpStatus.UNAUTHORIZED, "TICKET_ALREADY_USED", "Vé SSO đã được sử dụng"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Vai trò người dùng không hợp lệ hoặc chưa được cấp quyền"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Tài khoản của bạn đã bị tạm khóa 15 phút do nhập sai mật khẩu nhiều lần"),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID", "Mã đặt lại mật khẩu không hợp lệ"),
    RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "RESET_TOKEN_EXPIRED", "Mã đặt lại mật khẩu đã hết hạn (có hiệu lực trong 30 phút)"),
    RESET_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, "RESET_TOKEN_ALREADY_USED", "Mã đặt lại mật khẩu đã được sử dụng"),
    MUST_CHANGE_PASSWORD(HttpStatus.FORBIDDEN, "MUST_CHANGE_PASSWORD", "Bắt buộc phải đổi mật khẩu lần đầu trước khi tiếp tục truy cập"),
    EXTERNAL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_UNAVAILABLE", "Dịch vụ Mock LMS tạm thời không khả dụng. Vui lòng thử lại sau"),
    LMS_ERROR(HttpStatus.BAD_GATEWAY, "LMS_ERROR", "Dịch vụ Mock LMS phản hồi lỗi trong quá trình xác thực"),
    LMS_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "LMS_UNAUTHORIZED", "Dịch vụ Mock LMS từ chối quyền truy cập (HTTP 401)"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Đã xảy ra sự cố cơ sở dữ liệu"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
