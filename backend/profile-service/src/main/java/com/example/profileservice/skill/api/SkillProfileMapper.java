package com.example.profileservice.skill.api;

import com.example.profileservice.skill.api.dto.SkillEvidenceResponse;
import com.example.profileservice.skill.api.dto.SkillProfileResponse;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SkillProfileMapper {

    SkillProfileMapper INSTANCE = Mappers.getMapper(SkillProfileMapper.class);

    SkillEvidenceResponse toDto(SkillEvidence e);

    List<SkillEvidenceResponse> toDto(List<SkillEvidence> list);

    @Mapping(target = "skillCode", source = "skill.code")
    @Mapping(target = "skillName", source = "skill.name")
    @Mapping(target = "group", source = "skill.group")
    @Mapping(target = "verifiedScore", source = "skillScore.verifiedScore")
    @Mapping(target = "selfScore", source = "skillScore.selfScore")
    @Mapping(target = "level", source = "skillScore.level")
    SkillProfileResponse toDto(Skill skill, SkillScore skillScore, List<SkillEvidence> evidences);
}