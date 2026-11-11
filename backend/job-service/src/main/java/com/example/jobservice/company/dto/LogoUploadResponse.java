package com.example.jobservice.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LogoUploadResponse(
        @Schema(description = "Đường dẫn tương đối của logo; FE ghép với base URL của API")
        String logoUrl,
        String message
) {
}
