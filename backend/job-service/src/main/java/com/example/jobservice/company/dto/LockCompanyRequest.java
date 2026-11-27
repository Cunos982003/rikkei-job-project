package com.example.jobservice.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockCompanyRequest(
        @NotBlank(message = "Vui lòng nhập lý do khóa doanh nghiệp")
        @Size(max = 500, message = "Lý do khóa doanh nghiệp không được vượt quá 500 ký tự")
        String reason
) {}

