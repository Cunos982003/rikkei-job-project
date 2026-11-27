package com.example.ai_career_service.service;

import com.example.ai_career_service.enums.AIDraftSection;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UnavailableAIDraftDataSource implements AIDraftDataSource {

    @Override
    public JsonNode loadInputSnapshot(
            UUID userId,
            UUID cvId,
            AIDraftSection section,
            UUID projectId,
            UUID jobId) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CV, profile, and job-skill data integrations are not configured");
    }
}
