package com.example.learningservice.client;

import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.lms.LmsCourseDto;
import com.example.learningservice.model.dto.lms.LmsEnrollmentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LmsClient {

    private final RestClient restClient;
    private final String mockServerUrl;
    private final String serviceKey;

    public LmsClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.integrations.mock-server-url:http://localhost:9000}") String mockServerUrl,
            @Value("${app.integrations.lms-service-key:dev-key}") String serviceKey) {
        this.mockServerUrl = mockServerUrl;
        this.serviceKey = serviceKey;
        this.restClient = restClientBuilder
                .baseUrl(mockServerUrl)
                .defaultHeader("X-Service-Key", serviceKey)
                .build();
    }

    /**
     * Gọi Mock LMS để đăng ký khóa học bổ sung.
     * Nếu LMS trả về lỗi hoặc không phản hồi -> quăng BusinessException (BAD_GATEWAY)
     * để nghiệp vụ không lưu vào DB (AC4).
     */
    public LmsEnrollmentDto enrollCourse(String lmsUserId, String lmsCourseId) {
        log.info("Gọi Mock LMS POST /lms/enrollments cho studentId={} courseId={}", lmsUserId, lmsCourseId);
        try {
            return restClient.post()
                    .uri("/lms/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "studentId", lmsUserId,
                            "courseId", lmsCourseId
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errMsg = new String(resp.getBody().readAllBytes());
                        log.error("LMS mock phản hồi lỗi khi ghi danh: status={}, body={}", resp.getStatusCode(), errMsg);
                        throw new BusinessException(
                                "Hệ thống LMS từ chối ghi danh hoặc đang bận (" + resp.getStatusCode().value() + "): " + errMsg,
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(LmsEnrollmentDto.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi kết nối tới Mock LMS tại {}: {}", mockServerUrl, e.getMessage());
            throw new BusinessException(
                    "Không thể kết nối đến hệ thống đào tạo LMS (" + e.getMessage() + ")",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    /**
     * Lấy danh sách ghi danh và tiến độ khóa học của học viên từ Mock LMS.
     */
    public List<LmsEnrollmentDto> getStudentEnrollments(String lmsUserId) {
        log.debug("Gọi Mock LMS GET /lms/enrollments/{} để đồng bộ tiến độ", lmsUserId);
        try {
            List<LmsEnrollmentDto> enrollments = restClient.get()
                    .uri("/lms/enrollments/{lmsUserId}", lmsUserId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Mock LMS trả về lỗi khi lấy tiến độ studentId={}: status={}", lmsUserId, resp.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<List<LmsEnrollmentDto>>() {});

            return enrollments != null ? enrollments : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Không thể lấy tiến độ học từ Mock LMS cho studentId {}: {}", lmsUserId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Lấy danh mục khóa học từ Mock LMS.
     */
    public List<LmsCourseDto> getCourses() {
        log.debug("Gọi Mock LMS GET /lms/courses");
        try {
            List<LmsCourseDto> courses = restClient.get()
                    .uri("/lms/courses")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Mock LMS trả về mã lỗi khi lấy danh mục courses: {}", resp.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<List<LmsCourseDto>>() {});

            return courses != null ? courses : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Không thể lấy danh mục khóa học từ Mock LMS: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

