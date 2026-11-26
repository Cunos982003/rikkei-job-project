package com.example.learningservice.model.dto.response.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopMissingSkillDto {
    private String skillId;
    private String skillName;
    private Long missingCount;
    private Double percentage;
}
