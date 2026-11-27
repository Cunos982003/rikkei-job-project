package com.example.ai_career_service.service;

import java.util.UUID;

/**
 * Integration boundary for profile-service, job-service, and assessment-service.
 * Implementations must return only the authenticated user's verified indicators,
 * with DG-03 source weighting already applied and self-reported values excluded,
 * and approved, unexpired jobs with their required and preferred skill thresholds.
 * An unknown candidate must be reported as not found rather than as an empty snapshot;
 * callers must preserve that failure instead of returning a default score.
 */
public interface RecommendationDataSource {

    RecommendationSnapshot loadSnapshot(UUID userId);
}
