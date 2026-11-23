package com.example.profileservice.skill.api.dto;

public record SkillScoreInternalResponse(
                String skillId,
                String skillCode,
                String skillName,
                Integer verifiedScore,
                Integer selfScore) {

}
