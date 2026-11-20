package com.example.recruitment_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.recruitment_service.dto.RecruitmentReviewDetails;
import com.example.recruitment_service.dto.RecruitmentSummary;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.service.RecruitmentService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@ActiveProfiles("test")
class RecruitmentControllerTest {
    private static final UUID RECRUITMENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String JWT_SECRET = "test-only-signing-key-not-for-production";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @MockitoBean
    private RecruitmentService recruitmentService;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void allowsStaffReadButDeniesStaffMutation() throws Exception {
        when(recruitmentService.getReviewDetails(eq(RECRUITMENT_ID), any(UserPrincipal.class), anyString()))
                .thenReturn(reviewDetails());

        mockMvc.perform(get("/api/v1/recruitments/{id}", RECRUITMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.score").value(91.0));

        mockMvc.perform(post("/api/v1/recruitments/{id}/transition", RECRUITMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer("STAFF"))
                        .contentType("application/json")
                        .content("{\"toStatus\":\"REJECTED\",\"note\":\"reviewed\"}"))
                .andExpect(status().isForbidden());

        verify(recruitmentService, never()).transition(any(), any(), any(), anyString());
    }

    @Test
    void returnsInvalidTransitionCode() throws Exception {
        when(recruitmentService.transition(
                eq(RECRUITMENT_ID), any(), any(UserPrincipal.class), anyString()))
                .thenThrow(new ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "This recruitment status transition is not allowed",
                        "INVALID_TRANSITION"));

        mockMvc.perform(post("/api/v1/recruitments/{id}/transition", RECRUITMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer("HR"))
                        .contentType("application/json")
                        .content("{\"toStatus\":\"HIRED\",\"note\":\"invalid edge\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void allowsCandidateApplyAliasWithoutCvId() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(recruitmentService.recruit(eq(jobId), any(), any(UserPrincipal.class), anyString()))
                .thenReturn(new RecruitmentSummary(
                        RECRUITMENT_ID,
                        jobId,
                        null,
                        "/recruitments/" + RECRUITMENT_ID + "/cv-snapshot",
                        null,
                        RecruitmentStatus.SUBMITTED,
                        LocalDateTime.now(),
                        LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/jobs/{id}/apply", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("CANDIDATE"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    private RecruitmentReviewDetails reviewDetails() {
        return new RecruitmentReviewDetails(
                new RecruitmentSummary(
                        RECRUITMENT_ID,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "/submitted-cv.pdf",
                        "Cover letter",
                        RecruitmentStatus.SUBMITTED,
                        LocalDateTime.now(),
                        LocalDateTime.now()),
                USER_ID,
                "candidate@example.test",
                "Cover letter",
                "/submitted-cv.pdf",
                91.0,
                List.of(),
                List.of());
    }

    private String bearer(String role) {
        String token = Jwts.builder()
                .claim("userId", USER_ID.toString())
                .claim("role", role)
                .claim("companyId", COMPANY_ID.toString())
                .claim("email", "reviewer@example.test")
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        return "Bearer " + token;
    }
}
