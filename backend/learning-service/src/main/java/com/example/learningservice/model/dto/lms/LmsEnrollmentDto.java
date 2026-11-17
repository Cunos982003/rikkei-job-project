package com.example.learningservice.model.dto.lms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LmsEnrollmentDto {
    private String id;
    private String studentId;
    private String userId;
    private String courseId;
    private String lmsCourseId;
    private String status;
    private BigDecimal progress;
    private BigDecimal score;
    private Instant enrolledAt;
    private Instant completedAt;
}

