package com.example.recruitment_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.recruitment_service.dto.EmploymentRecordResponse;
import com.example.recruitment_service.dto.OfferResponse;
import com.example.recruitment_service.enums.OfferStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.service.OfferService;
import com.example.recruitment_service.service.RecruitmentService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@ActiveProfiles("test")
class OfferControllerTest {
    private static final UUID APP_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID HR_ID = UUID.randomUUID();
    private static final String JWT_SECRET = "test-only-signing-key-not-for-production";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @MockitoBean
    private OfferService offerService;

    @MockitoBean
    private RecruitmentService recruitmentService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void hrCanSendOfferAndGets201() throws Exception {
        OfferResponse response = sampleOffer(OfferStatus.SENT, RecruitmentStatus.OFFER, null);
        when(offerService.sendOffer(eq(APP_ID), any(), any(UserPrincipal.class), anyString())).thenReturn(response);

        String body = """
                {
                  "position": "Java Backend Developer",
                  "salary": 18000000,
                  "startDate": "2026-12-01",
                  "workLocation": "Hà Nội",
                  "responseDeadline": "%s",
                  "note": "Chào mừng gia nhập!"
                }
                """.formatted(Instant.now().plus(3, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/applications/{id}/offer", APP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(HR_ID, "HR", COMPANY_ID, "hr1@rikkeijobs.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.position").value("Java Backend Developer"));
    }

    @Test
    void candidateCannotSendOfferGets403() throws Exception {
        String body = """
                {
                  "position": "Java Backend Developer",
                  "salary": 18000000,
                  "startDate": "2026-12-01",
                  "workLocation": "Hà Nội",
                  "responseDeadline": "%s"
                }
                """.formatted(Instant.now().plus(3, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/applications/{id}/offer", APP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CANDIDATE_ID, "CANDIDATE", null, "candidate1@rikkeijobs.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void declineOfferWithBlankReasonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/me/offers/{id}/decline", OFFER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CANDIDATE_ID, "CANDIDATE", null, "candidate1@rikkeijobs.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherCandidateOpeningOfferReturns404() throws Exception {
        when(offerService.getCandidateOffer(eq(OFFER_ID), any(UserPrincipal.class)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        mockMvc.perform(get("/api/v1/me/offers/{id}", OFFER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "CANDIDATE", null, "candidate2@rikkeijobs.test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void candidateCanAcceptOfferAndFetchEmploymentRecords() throws Exception {
        when(offerService.acceptOffer(eq(OFFER_ID), any(UserPrincipal.class)))
                .thenReturn(sampleOffer(OfferStatus.ACCEPTED, RecruitmentStatus.HIRED, null));
        when(offerService.listMyEmploymentRecords(any(UserPrincipal.class)))
                .thenReturn(List.of(new EmploymentRecordResponse(
                        UUID.randomUUID(), CANDIDATE_ID, COMPANY_ID, "Rikkeisoft", JOB_ID, APP_ID,
                        "Java Backend Developer", LocalDate.of(2026, 12, 1), null, Instant.now())));

        mockMvc.perform(post("/api/v1/me/offers/{id}/accept", OFFER_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CANDIDATE_ID, "CANDIDATE", null, "candidate1@rikkeijobs.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.applicationStatus").value("HIRED"));

        mockMvc.perform(get("/api/v1/me/employment-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CANDIDATE_ID, "CANDIDATE", null, "candidate1@rikkeijobs.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].position").value("Java Backend Developer"))
                .andExpect(jsonPath("$.data[0].companyName").value("Rikkeisoft"));
    }

    @Test
    void hrCanConfirmOnboard() throws Exception {
        Instant confirmedAt = Instant.now();
        when(offerService.confirmOnboard(eq(APP_ID), any(UserPrincipal.class), anyString()))
                .thenReturn(new EmploymentRecordResponse(
                        UUID.randomUUID(), CANDIDATE_ID, COMPANY_ID, "Rikkeisoft", JOB_ID, APP_ID,
                        "Java Backend Developer", LocalDate.of(2026, 12, 1), confirmedAt, Instant.now()));

        mockMvc.perform(post("/api/v1/applications/{id}/offer/confirm-onboard", APP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(HR_ID, "HR", COMPANY_ID, "hr1@rikkeijobs.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedAt").isNotEmpty());
    }

    private OfferResponse sampleOffer(OfferStatus status, RecruitmentStatus appStatus, String declineReason) {
        return new OfferResponse(
                OFFER_ID, APP_ID, JOB_ID, COMPANY_ID, "Rikkeisoft", CANDIDATE_ID, "candidate1@rikkeijobs.test",
                "Java Backend Developer", new BigDecimal("18000000"), LocalDate.of(2026, 12, 1),
                "Hà Nội", "Chào mừng gia nhập!", Instant.now().plus(3, ChronoUnit.DAYS),
                status, appStatus, status == OfferStatus.SENT ? null : Instant.now(),
                declineReason, null, Instant.now(), Instant.now());
    }

    private String bearer(UUID userId, String role, UUID companyId, String email) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("role", role)
                .claim("email", email);
        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }
        return "Bearer " + builder.signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
