package com.example.recruitment_service.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.example.recruitment_service.exception.ApiException;

@Component
public class JobServiceClient {
    private final RestClient restClient;
    private final String jobPath;

    public JobServiceClient(
            @Value("${app.integrations.job-service-url}") String baseUrl,
            @Value("${app.integrations.job-details-path:/jobs/{jobId}}") String jobPath) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.jobPath = jobPath;
    }

    public JobDetails getJob(UUID jobId, String authorization) {
        JobDetails job;
        try {
            job = restClient.get()
                    .uri(jobPath, jobId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(JobDetails.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Job not found");
            }
            throw exception;
        }
        if (job == null) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Job service returned no job");
        }
        return job;
    }

    public record JobDetails(
            String status,
            Instant expiresAt,
            UUID companyId,
            List<String> hrEmails) {
    }
}
