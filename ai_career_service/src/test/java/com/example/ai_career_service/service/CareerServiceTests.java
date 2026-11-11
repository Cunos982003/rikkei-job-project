package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ai_career_service.dto.SkillGapResponse;
import com.example.ai_career_service.mapper.JobRecommendationMapper;
import com.example.ai_career_service.mapper.SkillGapMapper;
import com.example.ai_career_service.model.JobRecommendation;
import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.repository.JobRecommendationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareerServiceTests {

    private static final UUID USER_ID = new UUID(0, 100);

    private static UUID jobId(int index) {
        return new UUID(0, index);
    }

    @Mock
    private RecommendationDataSource dataSource;

    @Mock
    private JobRecommendationRepository repository;

    @Mock
    private AIProvider aiProvider;

    private final Executor directExecutor = Runnable::run;

    private CareerService createService() {
        return new CareerService(
                dataSource,
                repository,
                aiProvider,
                new JobRecommendationMapper(new SkillGapMapper()),
                directExecutor);
    }

    @Test
    void ranksResultsAndUsesAiOnlyForTopFive() {
        List<RecommendationSnapshot.EligibleJob> jobs = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> new RecommendationSnapshot.EligibleJob(
                        jobId(index),
                        "revision-" + index,
                        List.of(new RecommendationSnapshot.SkillRequirement(
                                "Java", 100 - index * 5, RecommendationSnapshot.RequirementType.REQUIRED))))
                .toList();
        when(dataSource.loadSnapshot(USER_ID))
                .thenReturn(new RecommendationSnapshot(
                        "profile-v1",
                        List.of(new RecommendationSnapshot.VerifiedSkill("Java", 50)),
                        jobs));
        when(repository.findByUserIdAndJobId(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(JobRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProvider.generateExplanation(any())).thenReturn("AI explanation");

        CareerService service = createService();
        var recommendations = service.getRecommendations(USER_ID, 10);

        assertThat(recommendations).extracting("jobId")
                .containsExactly(jobId(7), jobId(6), jobId(5), jobId(4), jobId(3), jobId(2), jobId(1));
        assertThat(recommendations).extracting("explanation")
                .filteredOn("AI explanation"::equals)
                .hasSize(5);
        verify(aiProvider, times(5)).generateExplanation(any());
        verify(repository, times(7)).save(any(JobRecommendation.class));
    }

    @Test
    void returnsDataBasedExplanationWhenAiProviderFails() {
        var job = new RecommendationSnapshot.EligibleJob(
                jobId(1),
                "revision-1",
                List.of(new RecommendationSnapshot.SkillRequirement(
                        "Java", 70, RecommendationSnapshot.RequirementType.REQUIRED)));
        when(dataSource.loadSnapshot(USER_ID))
                .thenReturn(new RecommendationSnapshot(
                        "profile-v1",
                        List.of(new RecommendationSnapshot.VerifiedSkill("Java", 80)),
                        List.of(job)));
        when(repository.findByUserIdAndJobId(USER_ID, jobId(1))).thenReturn(Optional.empty());
        when(repository.save(any(JobRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("provider unavailable"))
                .when(aiProvider).generateExplanation(any());

        CareerService service = createService();
        var recommendation = service.getRecommendations(USER_ID, 10).get(0);

        assertThat(recommendation.score()).isEqualTo(100);
        assertThat(recommendation.explanation()).contains("Java");
        assertThat(recommendation.explanation().split("\\s+")).hasSizeLessThanOrEqualTo(60);
    }

    @Test
    void returnsDn06AndSkillGapsIncludingUnmetRequiredSkills() {
        var job = new RecommendationSnapshot.EligibleJob(
                jobId(1),
                "revision-1",
                List.of(
                        new RecommendationSnapshot.SkillRequirement(
                                "Java", 80, RecommendationSnapshot.RequirementType.REQUIRED),
                        new RecommendationSnapshot.SkillRequirement(
                                "Docker", 50, RecommendationSnapshot.RequirementType.PREFERRED)));
        when(dataSource.loadSnapshot(USER_ID))
                .thenReturn(new RecommendationSnapshot(
                        "profile-v1",
                        List.of(new RecommendationSnapshot.VerifiedSkill("Java", 60)),
                        List.of(job)));
        when(repository.findByUserIdAndJobId(USER_ID, jobId(1))).thenReturn(Optional.empty());
        when(repository.save(any(JobRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiProvider.generateExplanation(any())).thenReturn("Skills need improvement");

        CareerService service = createService();
        var recommendation = service.getRecommendations(USER_ID, 10).get(0);

        assertThat(recommendation.score()).isEqualTo(37.5);
        assertThat(recommendation.missingRequiredSkills()).containsExactly("Java");
        assertThat(recommendation.skillGaps()).containsExactly(
                new SkillGapResponse(
                        "Java", 60, 80, 20, true),
                new SkillGapResponse(
                        "Docker", null, 50, 50, false));
    }
}
