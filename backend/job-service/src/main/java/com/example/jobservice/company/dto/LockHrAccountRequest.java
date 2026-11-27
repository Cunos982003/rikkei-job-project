package com.example.jobservice.company.dto;

import jakarta.validation.constraints.NotBlank;

public record LockHrAccountRequest(
        @NotBlank(message = "Lý do khóa tài khoản không được để trống")
        String reason
) {}

