package com.example.learningservice.controller;

import com.example.learningservice.advice.GlobalHandleException;
import com.example.learningservice.model.dto.response.TargetPositionResponse;
import com.example.learningservice.service.TargetPositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TargetPositionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TargetPositionService targetPositionService;

    @InjectMocks
    private TargetPositionController targetPositionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(targetPositionController)
                .setControllerAdvice(new GlobalHandleException())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/learning/target-positions - Trả về danh sách vị trí dạng ApiResponse chuẩn")
    void testGetAllPositions() throws Exception {
        UUID posId = UUID.randomUUID();
        TargetPositionResponse pos = TargetPositionResponse.builder()
                .id(posId)
                .code("JAVA_BACKEND_FRESHER")
                .name("Java Backend Fresher")
                .description("Vị trí backend")
                .skills(List.of(
                        TargetPositionResponse.PositionSkillItemResponse.builder()
                                .skillId("JAVA_CORE")
                                .skillName("Java Core")
                                .minScore(75)
                                .isMandatory(true)
                                .build()
                ))
                .build();

        when(targetPositionService.getAllPositions()).thenReturn(List.of(pos));

        mockMvc.perform(get("/api/v1/learning/target-positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data[0].code", is("JAVA_BACKEND_FRESHER")))
                .andExpect(jsonPath("$.data[0].skills[0].skillName", is("Java Core")));
    }
}
