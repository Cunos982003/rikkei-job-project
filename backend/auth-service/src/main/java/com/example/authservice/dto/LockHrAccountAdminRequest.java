package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockHrAccountAdminRequest(
    @NotBlank(message = "Lý do khóa không được để trống")
    @Size(max = 500, message = "Lý do khóa tối đa 500 ký tự")
    String reason
) {}

