package com.example.profileservice.dto;

public record SkillScoreInternalResponse(
                String skillId,
                String skillCode,
                String skillName,
                Integer verifiedScore,
                Integer selfScore) {

}
