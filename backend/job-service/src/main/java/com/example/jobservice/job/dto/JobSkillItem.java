package com.example.jobservice.job.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobSkillItem(
        @NotBlank(message = "Mã kỹ năng không được để trống")
        String skillId,

        @NotNull(message = "Trường required không được để trống")
        Boolean required,

        @NotNull(message = "Điểm tối thiểu không được để trống")
        @Min(value = 0, message = "Điểm tối thiểu phải từ 0 đến 100")
        @Max(value = 100, message = "Điểm tối thiểu phải từ 0 đến 100")
        Integer minScore
) {}

