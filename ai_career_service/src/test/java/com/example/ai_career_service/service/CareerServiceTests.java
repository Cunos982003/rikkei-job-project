package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ai_career_service.model.JobRecommendation;
import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.repository.JobRecommendationRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareerServiceTests {

    @Mock
    private RecommendationDataSource dataSource;

    @Mock
    private JobRecommendationRepository repository;

    @Mock
    private AIProvider aiProvider;

    private final Executor directExecutor = Runnable::run;

    @Test
    void ranksResultsAndUsesAiOnlyForTopFive() {
        List<RecommendationSnapshot.EligibleJob> jobs = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> new RecommendationSnapshot.EligibleJob(
                        "job-" + index,
                        "revision-" + index,
                        index,
                        List.of(new RecommendationSnapshot.SkillRequirement("Java", 50))))
                .toList();
        when(dataSource.loadSnapshot("candidate-1"))
                .thenReturn(new RecommendationSnapshot("profile-v1", List.of(), jobs));
        when(repository.findByUserIdAndJobId(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(JobRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProvider.generateExplanation(any())).thenReturn("AI explanation");

        CareerService service = new CareerService(dataSource, repository, aiProvider, directExecutor);
        var recommendations = service.getRecommendations("candidate-1", 10);

        assertThat(recommendations).extracting("jobId")
                .containsExactly("job-7", "job-6", "job-5", "job-4", "job-3", "job-2", "job-1");
        assertThat(recommendations).extracting("explanation")
                .filteredOn("AI explanation"::equals)
                .hasSize(5);
        verify(aiProvider, times(5)).generateExplanation(any());
        verify(repository, times(7)).save(any(JobRecommendation.class));
    }

    @Test
    void returnsDataBasedExplanationWhenAiProviderFails() {
        var job = new RecommendationSnapshot.EligibleJob(
                "job-1",
                "revision-1",
                72,
                List.of(new RecommendationSnapshot.SkillRequirement("Java", 70)));
        when(dataSource.loadSnapshot("candidate-1"))
                .thenReturn(new RecommendationSnapshot(
                        "profile-v1",
                        List.of(new RecommendationSnapshot.VerifiedSkill("Java", 80)),
                        List.of(job)));
        when(repository.findByUserIdAndJobId("candidate-1", "job-1")).thenReturn(Optional.empty());
        when(repository.save(any(JobRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("provider unavailable"))
                .when(aiProvider).generateExplanation(any());

        CareerService service = new CareerService(dataSource, repository, aiProvider, directExecutor);
        var recommendation = service.getRecommendations("candidate-1", 10).get(0);

        assertThat(recommendation.explanation()).contains("Java");
        assertThat(recommendation.explanation().split("\\s+")).hasSizeLessThanOrEqualTo(60);
    }
}
