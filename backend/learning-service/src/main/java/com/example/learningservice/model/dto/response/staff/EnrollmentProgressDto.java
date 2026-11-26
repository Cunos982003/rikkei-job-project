package com.example.learningservice.model.dto.response.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentProgressDto {
    private String course;
    private BigDecimal progress;
    private BigDecimal score;
    private String status;
}
