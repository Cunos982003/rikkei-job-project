package com.example.learningservice.model.dto.response.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpskillingCandidateDto {
    private CandidateInfoDto candidate;
    private String classCode;
    private String courseName;
    private String target;
    @Builder.Default
    private List<MissingSkillDto> missingSkills = new ArrayList<>();
    @Builder.Default
    private List<EnrollmentProgressDto> enrollments = new ArrayList<>();
    private Double readiness;
    private String status;
}
