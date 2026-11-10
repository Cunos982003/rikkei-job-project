package com.example.ai_career_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.ai_career_service.dto.JobRecommendationResponse;
import com.example.ai_career_service.service.CareerService;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AICareerControllerTests {

    @Mock
    private CareerService careerService;

    @InjectMocks
    private AICareerController controller;

    @Test
    void delegatesRecommendationsForAuthenticatedCandidate() {
        Principal principal = () -> "candidate-1";
        List<JobRecommendationResponse> expected = List.of(
                new JobRecommendationResponse("job-1", 87.5, "Strong skill match", Instant.parse("2026-01-01T00:00:00Z")));
        when(careerService.getRecommendations("candidate-1", 5)).thenReturn(expected);

        List<JobRecommendationResponse> actual = controller.getRecommendations(principal, 5);

        assertThat(actual).isSameAs(expected);
        verify(careerService).getRecommendations("candidate-1", 5);
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
}
