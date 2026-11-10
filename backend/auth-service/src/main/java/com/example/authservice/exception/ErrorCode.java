package com.example.authservice.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    TICKET_INVALID(HttpStatus.BAD_REQUEST, "TICKET_INVALID", "Invalid SSO ticket"),
    TICKET_EXPIRED(HttpStatus.BAD_REQUEST, "TICKET_EXPIRED", "SSO ticket has expired (valid for 60 seconds)"),
    TICKET_ALREADY_USED(HttpStatus.CONFLICT, "TICKET_ALREADY_USED", "SSO ticket has already been used"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Invalid or unauthorized user role"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is locked due to too many failed login attempts"),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID", "Invalid password reset token"),
    RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "RESET_TOKEN_EXPIRED", "Password reset token has expired (valid for 30 minutes)"),
    RESET_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, "RESET_TOKEN_ALREADY_USED", "Password reset token has already been used"),
    EXTERNAL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_UNAVAILABLE", "Mock LMS service is unavailable or timed out"),
    LMS_ERROR(HttpStatus.BAD_GATEWAY, "LMS_ERROR", "Mock LMS returned an error during verification"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "A database error occurred"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An internal server error occurred");

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
