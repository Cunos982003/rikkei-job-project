package com.example.ai_career_service.mapper;

import com.example.ai_career_service.dto.AIDraftResponse;
import com.example.ai_career_service.model.AIDraft;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AIDraftMapper {

    public AIDraftResponse toResponse(AIDraft draft) {
        List<String> warnings = new ArrayList<>();
        JsonNode warningNode = draft.getWarnings();
        if (warningNode != null && warningNode.isArray()) {
            for (JsonNode warning : warningNode) {
                if (!warning.isTextual()) {
                    throw new IllegalStateException("AI draft warnings must contain only strings");
                }
                warnings.add(warning.asText());
            }
        } else if (warningNode != null && !warningNode.isNull()) {
            throw new IllegalStateException("AI draft warnings must be stored as a JSON array");
        }

        return new AIDraftResponse(
                draft.getId(),
                draft.getCvId(),
                draft.getSection(),
                draft.getProjectId(),
                draft.getTargetJobId(),
                draft.getOutput(),
                List.copyOf(warnings),
                draft.getStatus(),
                draft.getCreatedAt());
    }
}
