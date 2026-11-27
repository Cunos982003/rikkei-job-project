package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedCourseItemResponse {
    private String lmsCourseId;
    private String courseName;
    private String lessonHint;
    private Integer levelFrom;
    private Integer levelTo;
    private String level;
    private Boolean isEnrolled;
    private UUID enrollmentId;
    private EnrollmentStatus enrollmentStatus;
    private BigDecimal progress;
    private BigDecimal score;
    private Instant enrolledAt;
    private Instant completedAt;
}

