package com.example.ai_career_service.service;

/**
 * Integration boundary for profile-service, job-service, and assessment-service.
 * Implementations must return only the authenticated user's verified indicators,
 * with DG-03 source weighting already applied and self-reported values excluded,
 * and approved, unexpired jobs with their required and preferred skill thresholds.
 */
public interface RecommendationDataSource {

    RecommendationSnapshot loadSnapshot(String userId);
}
