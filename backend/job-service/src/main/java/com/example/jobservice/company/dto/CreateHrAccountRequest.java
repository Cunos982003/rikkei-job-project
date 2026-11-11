package com.example.jobservice.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateHrAccountRequest(
                @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email,

                @NotBlank(message = "Họ và tên không được để trống") String fullName) {
}
