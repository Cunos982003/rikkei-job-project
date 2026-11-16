package com.example.ai_career_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ai_career_service.dto.JobRecommendationResponse;
import com.example.ai_career_service.service.CareerService;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AICareerControllerTests {

    private static final UUID USER_ID = new UUID(0, 100);
    private static final UUID JOB_ID = new UUID(0, 1);

    @Mock
    private CareerService careerService;

    @InjectMocks
    private AICareerController controller;

    @Test
    void bindsExplicitAndDefaultLimitsWithoutParameterNameMetadata() throws Exception {
        RequestParam requestParam = AICareerController.class
                .getMethod("getRecommendations", Principal.class, int.class)
                .getParameters()[1].getAnnotation(RequestParam.class);
        assertThat(requestParam.name()).isEqualTo("limit");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        Principal principal = USER_ID::toString;
        mockMvc.perform(get("/me/job-recommendations").principal(principal).param("limit", "5"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/me/job-recommendations").principal(principal))
                .andExpect(status().isOk());

        verify(careerService).getRecommendations(USER_ID, 5);
        verify(careerService).getRecommendations(USER_ID, 10);
    }

    @Test
    void delegatesRecommendationsForAuthenticatedCandidate() {
        Principal principal = USER_ID::toString;
        List<JobRecommendationResponse> expected = List.of(
                new JobRecommendationResponse(
                        JOB_ID,
                        87.5,
                        "Strong skill match",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        List.of(),
                        List.of()));
        when(careerService.getRecommendations(USER_ID, 5)).thenReturn(expected);

        List<JobRecommendationResponse> actual = controller.getRecommendations(principal, 5);

        assertThat(actual).isSameAs(expected);
        verify(careerService).getRecommendations(USER_ID, 5);
    }

    @Test
    void rejectsMissingPrincipal() {
        assertThatThrownBy(() -> controller.getRecommendations(null, 10))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(careerService);
    }

    @Test
    void rejectsPrincipalWithBlankName() {
        Principal principal = () -> " ";

        assertThatThrownBy(() -> controller.getRecommendations(principal, 10))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(careerService);
    }

    @Test
    void rejectsPrincipalWithoutCanonicalUuid() {
        for (String name : List.of("candidate-1", "1-1-1-1-1")) {
            Principal principal = () -> name;
            assertThatThrownBy(() -> controller.getRecommendations(principal, 10))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        verifyNoInteractions(careerService);
    }
}
