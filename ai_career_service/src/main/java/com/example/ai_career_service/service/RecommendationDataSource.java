package com.example.ai_career_service.service;

/** TODO: Implement the integration with profile-service, job-service, and assessment-service.
 * Integration boundary for profile-service, job-service, and assessment-service.
 * Implementations must return only authenticated user's verified indicators and
 * approved, unexpired jobs with DN-06 scores.
 */
public interface RecommendationDataSource {

    RecommendationSnapshot loadSnapshot(String userId);
}
