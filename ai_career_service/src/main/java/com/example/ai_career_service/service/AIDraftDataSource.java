package com.example.ai_career_service.service;

import com.example.ai_career_service.enums.AIDraftSection;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public interface AIDraftDataSource {

    /**
     * Loads only the candidate's authorized CV/profile facts and, when requested,
     * the target job's skill requirements. Do not include the full job description.
     */
    JsonNode loadInputSnapshot(
            UUID userId,
            UUID cvId,
            AIDraftSection section,
            UUID projectId,
            UUID jobId);
}
