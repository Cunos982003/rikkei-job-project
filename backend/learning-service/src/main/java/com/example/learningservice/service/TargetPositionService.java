package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.TargetPositionResponse;
import com.example.learningservice.model.entity.TargetPosition;

import java.util.List;
import java.util.UUID;

public interface TargetPositionService {

    List<TargetPositionResponse> getAllPositions();

    TargetPositionResponse getPositionResponseById(UUID id);

    TargetPosition getPositionEntityById(UUID id);
}

