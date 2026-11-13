package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateHrUserRequest(
    @NotBlank @Email String email,
    @NotBlank String fullName,
    @NotNull UUID companyId,
    @NotBlank String temporaryPassword
) {}

