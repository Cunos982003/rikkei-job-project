package com.example.learningservice.client;

import com.example.learningservice.exception.ResourceNotFoundException;
import com.example.learningservice.model.dto.response.JobDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class JobServiceClient {

    private final RestClient restClient;
    private final String jobServiceUrl;

    public JobServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.integrations.job-service-url:http://localhost:9000}") String jobServiceUrl) {
        this.jobServiceUrl = jobServiceUrl;
        this.restClient = restClientBuilder
                .baseUrl(jobServiceUrl)
                .build();
    }

    public JobDetailResponse getJobById(String jobId) {
        log.debug("Gọi dịch vụ việc làm tại {}/api/v1/jobs/{}", jobServiceUrl, jobId);
        try {
            return restClient.get()
                    .uri("/api/v1/jobs/{jobId}", jobId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với mã: " + jobId);
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Dịch vụ việc làm phản hồi mã lỗi {}", resp.getStatusCode());
                        throw new ResourceNotFoundException("Dịch vụ việc làm không khả dụng hoặc lỗi truy vấn mã: " + jobId);
                    })
                    .body(JobDetailResponse.class);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Không thể kết nối đến dịch vụ việc làm tại {} cho jobId {}: {}", jobServiceUrl, jobId, e.getMessage());
            throw new ResourceNotFoundException("Không thể lấy thông tin tin tuyển dụng với mã: " + jobId + " (" + e.getMessage() + ")");
        }
    }
}

