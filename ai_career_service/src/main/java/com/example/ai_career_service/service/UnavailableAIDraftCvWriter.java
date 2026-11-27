package com.example.ai_career_service.service;

import com.example.ai_career_service.model.AIDraft;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UnavailableAIDraftCvWriter implements AIDraftCvWriter {

    @Override
    public void writeDraft(AIDraft draft) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CV write integration is not configured");
    }
}
