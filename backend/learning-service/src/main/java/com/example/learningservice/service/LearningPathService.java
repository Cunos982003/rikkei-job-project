package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.LearningPathResponse;

import java.util.UUID;

public interface LearningPathService {

    LearningPathResponse generateLearningPath(UUID userId, String authHeader);

    LearningPathResponse getMyLearningPath(UUID userId, String authHeader);

    void archiveCurrentPath(UUID userId);

    void markStepDoneIfCourseCompleted(UUID userId, String lmsCourseId, String skillId);
}

