package com.example.profileservice.service;

import com.example.profileservice.common.domain.SkillId;
import com.example.profileservice.dto.SkillEvidenceResponse;
import com.example.profileservice.dto.SkillProfileResponse;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillEvidence;
import com.example.profileservice.entity.SkillScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SkillProfileMapper {

    SkillProfileMapper INSTANCE = Mappers.getMapper(SkillProfileMapper.class);

    SkillEvidenceResponse toDto(SkillEvidence e);

    List<SkillEvidenceResponse> toDto(List<SkillEvidence> list);

    @Mapping(target = "skillId", source = "skill.skillId")
    @Mapping(target = "skillCode", source = "skill.code")
    @Mapping(target = "skillName", source = "skill.name")
    @Mapping(target = "group", source = "skill.group")
    @Mapping(target = "verifiedScore", source = "skillScore.verifiedScore")
    @Mapping(target = "selfScore", source = "skillScore.selfScore")
    @Mapping(target = "level", expression = "java(skillScore.getLevel().label())")
    SkillProfileResponse toDto(Skill skill, SkillScore skillScore, List<SkillEvidence> evidences);

    default UUID map(SkillId skillId) {
        return skillId != null ? skillId.value() : null;
    }
}