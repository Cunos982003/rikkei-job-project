package com.example.learningservice.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileSkillResponse {
    private String skillId;
    private String skillCode;
    private String skillName;
    private String group;
    private Integer verifiedScore;
    private Integer selfScore;
    private String level;
}

