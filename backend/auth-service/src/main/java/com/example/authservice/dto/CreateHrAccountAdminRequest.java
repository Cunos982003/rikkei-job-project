package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateHrAccountAdminRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email,

    @NotBlank(message = "Họ tên không được để trống")
    String fullName,

    @NotNull(message = "ID công ty không được để trống")
    UUID companyId
) {}

