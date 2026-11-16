package com.example.learningservice.service.impl;

import com.example.learningservice.exception.ResourceNotFoundException;
import com.example.learningservice.model.dto.response.TargetPositionResponse;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.TargetPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetPositionServiceImpl implements TargetPositionService {

    private final TargetPositionRepository targetPositionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TargetPositionResponse> getAllPositions() {
        return targetPositionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TargetPositionResponse getPositionResponseById(UUID id) {
        TargetPosition position = getPositionEntityById(id);
        return mapToResponse(position);
    }

    @Override
    @Transactional(readOnly = true)
    public TargetPosition getPositionEntityById(UUID id) {
        return targetPositionRepository.findWithSkillsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí mục tiêu với ID: " + id));
    }

    private TargetPositionResponse mapToResponse(TargetPosition entity) {
        List<TargetPositionResponse.PositionSkillItemResponse> skillDtos = entity.getSkills() != null
                ? entity.getSkills().stream()
                .map(s -> TargetPositionResponse.PositionSkillItemResponse.builder()
                        .skillId(s.getId() != null ? s.getId().getSkillId() : null)
                        .skillName(s.getSkillName())
                        .minScore(s.getMinScore())
                        .isMandatory(s.getIsMandatory())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return TargetPositionResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .skills(skillDtos)
                .build();
    }
}

