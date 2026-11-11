package com.example.ai_career_service.service;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UnavailableRecommendationDataSource implements RecommendationDataSource {

    /**
     * TODO: Implement the integration with profile-service, job-service, and assessment-service.
     */
    @Override
    public RecommendationSnapshot loadSnapshot(UUID userId) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Profile, approved-job, and DN-06 data integrations are not configured");
    }
}
