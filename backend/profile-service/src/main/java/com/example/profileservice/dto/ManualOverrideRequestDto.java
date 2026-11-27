package com.example.profileservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualOverrideRequestDto {
    @NotBlank(message = "Field cannot be blank")
    private String field;

    @NotBlank(message = "Value cannot be blank")
    private String value;

    @NotBlank(message = "Reason is mandatory")
    private String reason;
}
