package com.example.authservice.metrics;

import com.example.authservice.exception.ErrorCode;

public enum AuthReason {
    NONE("none"),
    INVALID_CREDENTIALS("invalid_credentials"),
    ACCOUNT_LOCKED("account_locked"),
    TICKET_INVALID("ticket_invalid"),
    TICKET_EXPIRED("ticket_expired"),
    TICKET_ALREADY_USED("ticket_already_used"),
    INVALID_ROLE("invalid_role"),
    EXTERNAL_UNAVAILABLE("external_unavailable"),
    LMS_ERROR("lms_error"),
    VALIDATION_ERROR("validation_error"),
    INTERNAL_ERROR("internal_error");

    private final String value;

    AuthReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AuthReason fromErrorCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return INTERNAL_ERROR;
        }
        return switch (errorCode) {
            case INVALID_CREDENTIALS -> INVALID_CREDENTIALS;
            case ACCOUNT_LOCKED -> ACCOUNT_LOCKED;
            case TICKET_INVALID -> TICKET_INVALID;
            case TICKET_EXPIRED -> TICKET_EXPIRED;
            case TICKET_ALREADY_USED -> TICKET_ALREADY_USED;
            case INVALID_ROLE -> INVALID_ROLE;
            case EXTERNAL_UNAVAILABLE -> EXTERNAL_UNAVAILABLE;
            case LMS_ERROR, LMS_UNAUTHORIZED -> LMS_ERROR;
            default -> INTERNAL_ERROR;
        };
    }
}
