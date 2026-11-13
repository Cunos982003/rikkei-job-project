package com.example.learningservice.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobSkillResponse {
    private String skillId;
    private String skillName;
    private Integer minScore;
    @Builder.Default
    private Boolean isMandatory = true;
}

