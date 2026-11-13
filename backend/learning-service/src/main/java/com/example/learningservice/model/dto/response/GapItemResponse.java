package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.GapPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapItemResponse {
    private String skillId;
    private String skill;
    private String skillName;
    private Integer required;
    private Integer current;
    private Integer gap;
    private GapPriority priority;
    private Boolean isMandatory;
}

