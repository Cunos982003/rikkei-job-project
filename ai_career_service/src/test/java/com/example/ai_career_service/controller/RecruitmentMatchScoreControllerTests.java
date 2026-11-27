package com.example.ai_career_service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ai_career_service.service.CareerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RecruitmentMatchScoreControllerTests {

    private static final UUID USER_ID = new UUID(0, 100);
    private static final UUID JOB_ID = new UUID(0, 1);
    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Mock
    private CareerService careerService;

    @Test
    void rejectsMissingOrIncorrectInternalToken() throws Exception {
        MockMvc mockMvc = mockMvc(INTERNAL_TOKEN);

        mockMvc.perform(get(path()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(path()).header("X-Internal-Service-Token", "incorrect-token"))
                .andExpect(status().isUnauthorized());
        mockMvc("")
                .perform(get(path()).header("X-Internal-Service-Token", INTERNAL_TOKEN))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(careerService);
    }

    @Test
    void returnsScoreForValidInternalToken() throws Exception {
        when(careerService.getMatchScore(USER_ID, JOB_ID)).thenReturn(87.5);

        mockMvc(INTERNAL_TOKEN)
                .perform(get(path()).header("X-Internal-Service-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.jobId").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.data.score").value(87.5));

        verify(careerService).getMatchScore(USER_ID, JOB_ID);
    }

    private MockMvc mockMvc(String token) {
        return MockMvcBuilders.standaloneSetup(new RecruitmentMatchScoreController(careerService, token)).build();
    }

    private static String path() {
        return "/api/v1/recruitments/" + USER_ID + "/jobs/" + JOB_ID + "/match-score";
    }
}
