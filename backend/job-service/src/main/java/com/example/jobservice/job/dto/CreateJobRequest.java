package com.example.jobservice.job.dto;

import com.example.jobservice.job.domain.JobLevel;
import com.example.jobservice.job.domain.JobType;
import com.example.jobservice.job.domain.WorkMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateJobRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
        String title,

        @NotBlank(message = "Vị trí không được để trống")
        @Size(max = 255, message = "Vị trí không được vượt quá 255 ký tự")
        String position,

        @NotNull(message = "Cấp bậc không được để trống")
        JobLevel level,

        @NotNull(message = "Hình thức làm việc không được để trống")
        JobType type,

        @NotNull(message = "Số lượng tuyển không được để trống")
        @Min(value = 1, message = "Số lượng tuyển phải lớn hơn hoặc bằng 1")
        Integer quantity,

        String city,
        String address,

        @NotNull(message = "Chế độ làm việc không được để trống")
        WorkMode workMode,

        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String description,
        String requirements,
        String benefits,

        @NotNull(message = "Hạn nộp hồ sơ không được để trống")
        LocalDate deadline
) {}

