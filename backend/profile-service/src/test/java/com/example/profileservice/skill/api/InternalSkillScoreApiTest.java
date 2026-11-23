package com.example.profileservice.skill.api;

import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InternalSkillScoreApiTest {

    @Mock
    private SkillScoreRepository scoreRepo;

    @Mock
    private SkillRepository skillRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalSkillScoreController(scoreRepo, skillRepo)).build();
    }

    @Test
    @DisplayName("401 khi không có token")
    void getSkillScores_unauthorized_whenNoToken() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/profiles/internal/{userId}/skill-scores", userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    @Test
    @DisplayName("200 trả về danh sách điểm đúng các trường khi có token")
    void getSkillScores_success_withToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        Skill skill = Skill.builder()
                .id(skillId)
                .code("JAVA_CORE")
                .name("Java Core")
                .group("Programming")
                .build();

        SkillScore score = SkillScore.builder()
                .userId(userId)
                .skillId(skillId)
                .verifiedScore((short) 85)
                .selfScore((short) 90)
                .level("Vững")
                .updatedAt(OffsetDateTime.now())
                .build();

        when(scoreRepo.findByUserId(userId)).thenReturn(List.of(score));
        when(skillRepo.findById(skillId)).thenReturn(Optional.of(skill));

        mockMvc.perform(get("/api/v1/profiles/internal/{userId}/skill-scores", userId)
                        .header("Authorization", "Bearer valid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].skillId").value(skillId.toString()))
                .andExpect(jsonPath("$.data[0].skillCode").value("JAVA_CORE"))
                .andExpect(jsonPath("$.data[0].skillName").value("Java Core"))
                .andExpect(jsonPath("$.data[0].verifiedScore").value(85))
                .andExpect(jsonPath("$.data[0].selfScore").value(90));
    }

    @Test
    @DisplayName("200 trả về mảng rỗng khi userId không có điểm nào")
    void getSkillScores_emptyList_whenNoScores() throws Exception {
        UUID userId = UUID.randomUUID();
        when(scoreRepo.findByUserId(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/profiles/internal/{userId}/skill-scores", userId)
                        .header("Authorization", "Bearer valid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}

