package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.JobRecommendationResponse;
import com.example.ai_career_service.dto.SkillGapResponse;
import com.example.ai_career_service.dto.prompt.RecommendationPrompt;
import com.example.ai_career_service.model.JobRecommendation;
import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.repository.JobRecommendationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerService {

    private static final int AI_EXPLANATION_LIMIT = 5;
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final long AI_TIMEOUT_SECONDS = 10;

    private final RecommendationDataSource dataSource;
    private final JobRecommendationRepository repository;
    private final AIProvider aiProvider;
    private final Executor aiExecutor;

    public List<JobRecommendationResponse> getRecommendations(UUID userId, int limit) {
        RecommendationSnapshot snapshot = dataSource.loadSnapshot(userId);
        Instant now = Instant.now();
        List<ScoredJob> jobs = snapshot.eligibleJobs().stream()
                .map(job -> new ScoredJob(job, JobMatchScorer.score(snapshot.verifiedSkills(), job.requirements())))
                .sorted((first, second) -> Double.compare(second.match().score(), first.match().score()))
                .limit(limit)
                .toList();

        List<PendingRecommendation> pending = new java.util.ArrayList<>();
        JobRecommendationResponse[] results = new JobRecommendationResponse[jobs.size()];
        for (int index = 0; index < jobs.size(); index++) {
            ScoredJob scoredJob = jobs.get(index);
            RecommendationSnapshot.EligibleJob job = scoredJob.job();
            RecommendationPrompt prompt = RecommendationExplanation.createPrompt(snapshot, job);
            boolean aiExplanation = index < AI_EXPLANATION_LIMIT;
            String inputHash = inputHash(snapshot, job, aiExplanation);
            var cached = repository.findByUserIdAndJobId(userId, job.jobId())
                    .filter(item -> item.getCreatedAt().isAfter(now.minus(CACHE_TTL)))
                    .filter(item -> item.getInputHash().equals(inputHash));

            if (cached.isPresent()) {
                JobRecommendation recommendation = cached.get();
                results[index] = toResponse(recommendation, scoredJob.match());
                continue;
            }

            String fallback = RecommendationExplanation.fallback(prompt);
            CompletableFuture<String> explanation = aiExplanation
                    ? CompletableFuture.supplyAsync(
                                    () -> aiProvider.generateExplanation(prompt), aiExecutor)
                            .orTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .handle((text, error) -> {
                                if (error != null || text == null || text.isBlank()) {
                                    log.warn(
                                            "AI explanation unavailable for job {}; using data-based fallback",
                                            job.jobId(),
                                            error);
                                    return fallback;
                                }
                                return text;
                            })
                    : CompletableFuture.completedFuture(fallback);

            pending.add(new PendingRecommendation(
                    index, job, scoredJob.match(), inputHash, cached.orElse(null), explanation, now));
        }
        for (PendingRecommendation item : pending) {
            JobRecommendation recommendation = item.cached() == null ? new JobRecommendation() : item.cached();
            recommendation.setUserId(userId);
            recommendation.setJobId(item.job().jobId());
            recommendation.setScore(item.match().score());
            recommendation.setExplanation(RecommendationExplanation.limitWords(item.explanation().join()));
            recommendation.setCreatedAt(item.createdAt());
            recommendation.setInputHash(item.inputHash());
            results[item.index()] = toResponse(repository.save(recommendation), item.match());
        }
        return java.util.Arrays.asList(results);
    }

    private record ScoredJob(RecommendationSnapshot.EligibleJob job, JobMatchScorer.MatchResult match) {
    }

    private record PendingRecommendation(
            int index,
            RecommendationSnapshot.EligibleJob job,
            JobMatchScorer.MatchResult match,
            String inputHash,
            JobRecommendation cached,
            CompletableFuture<String> explanation,
            Instant createdAt) {
    }

    private JobRecommendationResponse toResponse(
            JobRecommendation recommendation,
            JobMatchScorer.MatchResult match) {
        return new JobRecommendationResponse(
                recommendation.getJobId(),
                recommendation.getScore(),
                recommendation.getExplanation(),
                recommendation.getCreatedAt(),
                match.missingRequiredSkills(),
                match.skillGaps().stream()
                        .map(gap -> new SkillGapResponse(
                                gap.name(),
                                gap.verifiedIndex(),
                                gap.requiredIndex(),
                                gap.gap(),
                                gap.type() == RecommendationSnapshot.RequirementType.REQUIRED))
                        .toList());
    }

    private String inputHash(
            RecommendationSnapshot snapshot,
            RecommendationSnapshot.EligibleJob job,
            boolean aiExplanation) {
        String canonical = "dn06-v1|" + snapshot.profileRevision() + "|" + job.jobRevision()
                + "|aiExplanation=" + aiExplanation + "|"
                + snapshot.verifiedSkills().stream()
                        .sorted(java.util.Comparator.comparing(RecommendationSnapshot.VerifiedSkill::name))
                        .map(skill -> skill.name() + ":" + skill.index())
                        .reduce("", (first, second) -> first + "|" + second)
                + job.requirements().stream()
                        .sorted(java.util.Comparator.comparing(RecommendationSnapshot.SkillRequirement::name))
                        .map(skill -> "|" + skill.name() + ":" + skill.requiredIndex() + ":" + skill.type())
                        .reduce("", String::concat);
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
