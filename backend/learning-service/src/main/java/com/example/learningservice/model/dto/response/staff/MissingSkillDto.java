package com.example.learningservice.model.dto.response.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingSkillDto {
    private String skillId;
    private String skillName;
    private Integer required;
    private Integer current;
    private Integer gap;
    private String priority;
}
