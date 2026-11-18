package com.example.profileservice.skill.api;

import com.example.profileservice.skill.api.dto.SkillCatalogResponse;
import com.example.profileservice.skill.application.SkillCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SkillCatalogControllerTest {

    @Mock
    private SkillCatalogService skillCatalogService;

    private MockMvc mockMvc;

    private final List<SkillCatalogResponse> sampleSkills = List.of(
            new SkillCatalogResponse(UUID.randomUUID().toString(), "SPRING_BOOT", "Spring Boot", "BACKEND"),
            new SkillCatalogResponse(UUID.randomUUID().toString(), "SQL", "SQL / Database", "DATABASE"),
            new SkillCatalogResponse(UUID.randomUUID().toString(), "REACT", "ReactJS", "FRONTEND")
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SkillCatalogController(skillCatalogService)).build();
    }

    @Test
    @DisplayName("401 khi không truyền X-User-Id hoặc Authorization header")
    void getSkills_unauthorized_whenNoAuthHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Yêu cầu đăng nhập để truy cập danh mục kỹ năng"));
    }

    @Test
    @DisplayName("200 trả về danh sách kỹ năng chuẩn khi có header Authorization")
    void getSkills_success_withAuthorizationHeader() throws Exception {
        when(skillCatalogService.getSkills(null, null)).thenReturn(sampleSkills);

        mockMvc.perform(get("/api/v1/skills")
                        .header("Authorization", "Bearer valid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh mục kỹ năng thành công"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].code").value("SPRING_BOOT"))
                .andExpect(jsonPath("$.data[0].group").value("BACKEND"))
                .andExpect(jsonPath("$.data[1].code").value("SQL"))
                .andExpect(jsonPath("$.data[2].code").value("REACT"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("200 trả về danh sách kỹ năng khi có header X-User-Id")
    void getSkills_success_withXUserIdHeader() throws Exception {
        when(skillCatalogService.getSkills(null, null)).thenReturn(sampleSkills);

        mockMvc.perform(get("/api/v1/skills")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @DisplayName("200 lọc theo ?group=BACKEND")
    void getSkills_filterByGroup() throws Exception {
        List<SkillCatalogResponse> backendSkills = List.of(sampleSkills.get(0));
        when(skillCatalogService.getSkills(eq("BACKEND"), eq(null))).thenReturn(backendSkills);

        mockMvc.perform(get("/api/v1/skills")
                        .header("Authorization", "Bearer dummy")
                        .param("group", "BACKEND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("SPRING_BOOT"));
    }

    @Test
    @DisplayName("200 tìm kiếm theo ?q=react")
    void getSkills_searchByQuery() throws Exception {
        List<SkillCatalogResponse> reactSkills = List.of(sampleSkills.get(2));
        when(skillCatalogService.getSkills(eq(null), eq("react"))).thenReturn(reactSkills);

        mockMvc.perform(get("/api/v1/skills")
                        .header("Authorization", "Bearer dummy")
                        .param("q", "react"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("REACT"));
    }
}

