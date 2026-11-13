package com.example.learningservice.service;

import com.example.learningservice.model.dto.request.TargetSelectionRequest;
import com.example.learningservice.model.dto.response.CandidateTargetResponse;
import com.example.learningservice.model.entity.CandidateTarget;

import java.util.Optional;
import java.util.UUID;

public interface CandidateTargetService {

    CandidateTargetResponse setTarget(UUID userId, TargetSelectionRequest request);

    Optional<CandidateTargetResponse> getCurrentTarget(UUID userId);

    Optional<CandidateTarget> getTargetEntity(UUID userId);
}

