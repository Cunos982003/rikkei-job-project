package com.example.learningservice.controller;

import com.example.learningservice.advice.GlobalHandleException;
import com.example.learningservice.model.dto.response.CandidateTargetResponse;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.GapItemResponse;
import com.example.learningservice.model.dto.response.TargetInfoResponse;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.CandidateTargetService;
import com.example.learningservice.service.GapAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GapAnalysisControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CandidateTargetService candidateTargetService;

    @Mock
    private GapAnalysisService gapAnalysisService;

    @Spy
    private UserContextHelper userContextHelper = new UserContextHelper();

    @InjectMocks
    private GapAnalysisController gapAnalysisController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gapAnalysisController)
                .setControllerAdvice(new GlobalHandleException())
                .build();
    }

    @Test
    @DisplayName("PUT /api/v1/learning/me/target - Thiết lập mục tiêu thành công trả về HTTP 200")
    void testSetTarget_Success() throws Exception {
        UUID positionId = UUID.randomUUID();
        CandidateTargetResponse targetResp = CandidateTargetResponse.builder()
                .userId(UUID.randomUUID())
                .targetType(TargetType.POSITION)
                .positionId(positionId)
                .targetName("Java Backend Fresher")
                .chosenAt(Instant.now())
                .build();

        when(candidateTargetService.setTarget(any(), any())).thenReturn(targetResp);

        Map<String, Object> body = Map.of("positionId", positionId.toString());

        mockMvc.perform(put("/api/v1/learning/me/target")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-User-Id", "cand-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.targetType", is("POSITION")))
                .andExpect(jsonPath("$.data.targetName", is("Java Backend Fresher")));
    }

    @Test
    @DisplayName("PUT /api/v1/learning/me/target - Gửi cả 2 hoặc không gửi cái nào bị validation lỗi 400")
    void testSetTarget_ValidationFail() throws Exception {
        Map<String, Object> invalidBody = Map.of(
                "positionId", UUID.randomUUID().toString(),
                "jobId", "job-001"
        );

        mockMvc.perform(put("/api/v1/learning/me/target")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "cand-001")
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/learning/me/gap-analysis - Chưa đăng nhập (thiếu header) trả về HTTP 401")
    void testGetGapAnalysis_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/learning/me/gap-analysis"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(401)));
    }

    @Test
    @DisplayName("PUT /api/v1/learning/me/target - Sai vai trò (HR thay vì CANDIDATE) trả về HTTP 403")
    void testSetTarget_Forbidden() throws Exception {
        Map<String, Object> body = Map.of("positionId", UUID.randomUUID().toString());

        mockMvc.perform(put("/api/v1/learning/me/target")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "hr-001")
                        .header("X-User-Role", "HR")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }

    @Test
    @DisplayName("GET /api/v1/learning/me/target - Lấy mục tiêu hiện tại của ứng viên")
    void testGetCurrentTarget() throws Exception {
        CandidateTargetResponse targetResp = CandidateTargetResponse.builder()
                .userId(UUID.randomUUID())
                .targetType(TargetType.POSITION)
                .positionId(UUID.randomUUID())
                .targetName("Java Backend Fresher")
                .chosenAt(Instant.now())
                .build();

        when(candidateTargetService.getCurrentTarget(any())).thenReturn(Optional.of(targetResp));

        mockMvc.perform(get("/api/v1/learning/me/target")
                        .header("X-User-Id", "cand-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.targetName", is("Java Backend Fresher")));
    }

    @Test
    @DisplayName("GET /api/v1/learning/me/gap-analysis - Phân tích gap trả về tỷ lệ readiness và danh sách items")
    void testGetGapAnalysis() throws Exception {
        GapAnalysisResponse gapResponse = GapAnalysisResponse.builder()
                .target(TargetInfoResponse.builder()
                        .type(TargetType.POSITION)
                        .id("11111111-1111-1111-1111-111111111101")
                        .name("Java Backend Fresher")
                        .build())
                .readiness(80.0)
                .totalRequiredSkills(5)
                .passedSkillsCount(4)
                .missingSkillsCount(1)
                .computedAt(Instant.now())
                .items(List.of(
                        GapItemResponse.builder()
                                .skillId("REST_API")
                                .skill("REST API")
                                .skillName("REST API")
                                .required(70)
                                .current(0)
                                .gap(70)
                                .priority(GapPriority.HIGH)
                                .isMandatory(true)
                                .build()
                ))
                .build();

        when(gapAnalysisService.analyzeGap(any(), any(), any())).thenReturn(gapResponse);

        mockMvc.perform(get("/api/v1/learning/me/gap-analysis")
                        .header("X-User-Id", "cand-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.readiness", is(80.0)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].skill", is("REST API")))
                .andExpect(jsonPath("$.data.items[0].priority", is("HIGH")));
    }
}
