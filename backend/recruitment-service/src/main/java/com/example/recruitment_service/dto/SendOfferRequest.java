package com.example.recruitment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendOfferRequest(
        @NotBlank(message = "Vị trí tuyển dụng là bắt buộc")
        @Size(max = 255, message = "Vị trí tối đa 255 ký tự")
        String position,

        @NotNull(message = "Mức lương là bắt buộc")
        @DecimalMin(value = "0.01", message = "Mức lương phải lớn hơn 0")
        BigDecimal salary,

        @NotNull(message = "Ngày bắt đầu làm việc là bắt buộc")
        LocalDate startDate,

        @NotBlank(message = "Địa điểm làm việc là bắt buộc")
        @Size(max = 500, message = "Địa điểm làm việc tối đa 500 ký tự")
        String workLocation,

        @NotNull(message = "Hạn trả lời offer là bắt buộc")
        Instant responseDeadline,

        @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
        String note,

        @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
        String companyName
) {
}
