package com.example.profileservice.skill.application;

import com.example.profileservice.skill.api.dto.SkillCatalogResponse;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillCatalogService {

    private final SkillRepository skillRepository;

    public List<SkillCatalogResponse> getSkills(String group, String q) {
        List<Skill> skills = skillRepository.findAll();
        return skills.stream()
                .filter(s -> group == null || group.isBlank() || 
                        (s.getGroup() != null && s.getGroup().equalsIgnoreCase(group.trim())))
                .filter(s -> {
                    if (q == null || q.isBlank()) return true;
                    String query = q.trim().toLowerCase();
                    return (s.getName() != null && s.getName().toLowerCase().contains(query))
                            || (s.getCode() != null && s.getCode().toLowerCase().contains(query));
                })
                .sorted(Comparator.comparing(Skill::getGroup, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Skill::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(s -> new SkillCatalogResponse(
                        s.getId() != null ? s.getId().toString() : null,
                        s.getCode(),
                        s.getName(),
                        s.getGroup()
                ))
                .toList();
    }
}

