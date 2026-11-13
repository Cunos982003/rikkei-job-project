package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.GapAnalysisResponse;

import java.util.UUID;

public interface GapAnalysisService {

    GapAnalysisResponse analyzeGap(UUID userId, String jobIdOverride, String authHeader);
}

