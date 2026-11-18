package com.example.recruitment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeclineOfferRequest(
        @NotBlank(message = "Lý do từ chối offer là bắt buộc")
        @Size(max = 1000, message = "Lý do từ chối tối đa 1000 ký tự")
        String reason
) {
}
