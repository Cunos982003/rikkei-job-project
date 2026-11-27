package com.example.learningservice.controller;

import com.example.learningservice.advice.GlobalHandleException;
import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.response.LearningPathResponse;
import com.example.learningservice.model.dto.response.LearningPathStepResponse;
import com.example.learningservice.model.entity.LearningPathStatus;
import com.example.learningservice.model.entity.StepStatus;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.LearningPathService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LearningPathControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LearningPathService learningPathService;

    @Spy
    private UserContextHelper userContextHelper = new UserContextHelper();

    @InjectMocks
    private LearningPathController controller;

    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandleException())
                .build();
    }

    @Test
    @DisplayName("DT-03 API: POST /me/learning-path sinh lộ trình thành công trả về 201 Created")
    void generateLearningPath_success() throws Exception {
        LearningPathStepResponse step = LearningPathStepResponse.builder()
                .id(UUID.randomUUID())
                .stepOrder(1)
                .skillId("JAVA_CORE")
                .skillName("Java Core")
                .isMandatory(true)
                .gap(20)
                .lmsCourseId("CRS-01")
                .courseName("Lập trình Spring Boot nâng cao")
                .status(StepStatus.TODO)
                .build();

        LearningPathResponse response = LearningPathResponse.builder()
                .id(UUID.randomUUID())
                .userId(CANDIDATE_ID)
                .targetType(TargetType.POSITION)
                .targetId("pos-1")
                .targetName("Java Backend Fresher")
                .readinessAtStart(50.0)
                .currentReadiness(50.0)
                .readinessGrowth(0.0)
                .completionPercentage(0.0)
                .totalSteps(1)
                .completedSteps(0)
                .status(LearningPathStatus.ACTIVE)
                .createdAt(Instant.now())
                .steps(List.of(step))
                .build();

        when(learningPathService.generateLearningPath(eq(CANDIDATE_ID), any())).thenReturn(response);

        mockMvc.perform(post("/me/learning-path")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.message", is("Sinh lộ trình học tập tối ưu thành công")))
                .andExpect(jsonPath("$.data.targetName", is("Java Backend Fresher")))
                .andExpect(jsonPath("$.data.readinessAtStart", is(50.0)))
                .andExpect(jsonPath("$.data.totalSteps", is(1)))
                .andExpect(jsonPath("$.data.steps", hasSize(1)))
                .andExpect(jsonPath("$.data.steps[0].skillId", is("JAVA_CORE")))
                .andExpect(jsonPath("$.data.steps[0].status", is("TODO")));
    }

    @Test
    @DisplayName("DT-03 API: GET /me/learning-path lấy lộ trình hiện tại trả về 200 OK")
    void getMyLearningPath_success_whenExists() throws Exception {
        LearningPathResponse response = LearningPathResponse.builder()
                .id(UUID.randomUUID())
                .userId(CANDIDATE_ID)
                .targetType(TargetType.POSITION)
                .targetId("pos-1")
                .targetName("Java Backend Fresher")
                .readinessAtStart(50.0)
                .currentReadiness(65.0)
                .readinessGrowth(15.0)
                .completionPercentage(50.0)
                .totalSteps(2)
                .completedSteps(1)
                .status(LearningPathStatus.ACTIVE)
                .createdAt(Instant.now())
                .steps(List.of(
                        LearningPathStepResponse.builder().stepOrder(1).skillId("JAVA_CORE").status(StepStatus.DONE).build(),
                        LearningPathStepResponse.builder().stepOrder(2).skillId("SPRING_BOOT").status(StepStatus.DOING).build()
                ))
                .build();

        when(learningPathService.getMyLearningPath(eq(CANDIDATE_ID), any())).thenReturn(response);

        mockMvc.perform(get("/me/learning-path")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.message", is("Lấy lộ trình học tập thành công")))
                .andExpect(jsonPath("$.data.completionPercentage", is(50.0)))
                .andExpect(jsonPath("$.data.readinessGrowth", is(15.0)))
                .andExpect(jsonPath("$.data.steps", hasSize(2)))
                .andExpect(jsonPath("$.data.steps[0].status", is("DONE")));
    }

    @Test
    @DisplayName("DT-03 API: GET /me/learning-path khi chưa có lộ trình trả về 200 OK kèm data null")
    void getMyLearningPath_success_whenNotExists() throws Exception {
        when(learningPathService.getMyLearningPath(eq(CANDIDATE_ID), any())).thenReturn(null);

        mockMvc.perform(get("/me/learning-path")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.message", is("Chưa có lộ trình học tập nào được tạo")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    @DisplayName("DT-03 API: POST /me/learning-path trả về 400 Bad Request khi chưa thiết lập mục tiêu")
    void generateLearningPath_businessException_whenNoTarget() throws Exception {
        when(learningPathService.generateLearningPath(eq(CANDIDATE_ID), any()))
                .thenThrow(new BusinessException("Vui lòng thiết lập mục tiêu nghề nghiệp trước khi sinh lộ trình học tập."));

        mockMvc.perform(post("/me/learning-path")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(400)))
                .andExpect(jsonPath("$.message", is("Vui lòng thiết lập mục tiêu nghề nghiệp trước khi sinh lộ trình học tập.")));
    }
}
