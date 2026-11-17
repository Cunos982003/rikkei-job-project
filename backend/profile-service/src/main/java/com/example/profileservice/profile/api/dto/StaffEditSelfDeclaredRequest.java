package com.example.profileservice.profile.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffEditSelfDeclaredRequest {

    @NotNull(message = "Trường cần sửa không được để trống")
    private Field field;

    @NotBlank(message = "Giá trị mới không được để trống")
    private String value;

    @NotBlank(message = "Lý do sửa không được để trống")
    private String reason;

    public enum Field {
        HEADLINE,
        SUMMARY,
        SELF_SKILLS
    }
}