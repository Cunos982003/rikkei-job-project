package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.StepStatus;
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
public class LearningPathStepResponse {
    private UUID id;
    private Integer stepOrder;
    private String skillId;
    private String skillName;
    private Integer requiredScore;
    private Integer currentScore;
    private Integer gap;
    private Boolean isMandatory;
    private String lmsCourseId;
    private String courseName;
    private String lessonHint;
    private StepStatus status;
    private Instant doneAt;
    private BigDecimal progress;
    private BigDecimal score;
}

