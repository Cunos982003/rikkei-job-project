package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationExplanationTests {

    @Test
    void buildsPromptOnlyFromSkillNamesAndVerifiedIndexes() {
        var snapshot = new RecommendationSnapshot(
                "profile-v1",
                List.of(
                        new RecommendationSnapshot.VerifiedSkill("Java", 85),
                        new RecommendationSnapshot.VerifiedSkill("Kotlin", 40)),
                List.of());
        var job = new RecommendationSnapshot.EligibleJob(
                new UUID(0, 1),
                "job-v1",
                List.of(
                        new RecommendationSnapshot.SkillRequirement(
                                "Java", 70, RecommendationSnapshot.RequirementType.REQUIRED),
                        new RecommendationSnapshot.SkillRequirement(
                                "Kotlin", 60, RecommendationSnapshot.RequirementType.PREFERRED),
                        new RecommendationSnapshot.SkillRequirement(
                                "SQL", 50, RecommendationSnapshot.RequirementType.REQUIRED)));

        String prompt = RecommendationExplanation.createPrompt(snapshot, job).toUserPrompt();

        assertThat(prompt).contains("Java", "85", "70", "bắt buộc", "Kotlin", "40", "60", "ưu tiên", "SQL", "chưa có");
        assertThat(prompt).doesNotContain("profile-v1", "job-1", "job-v1");
    }

    @Test
    void fallbackIsConciseAndCoversFitAndGaps() {
        var snapshot = new RecommendationSnapshot(
                "profile-v1",
                List.of(new RecommendationSnapshot.VerifiedSkill("Java", 85)),
                List.of());
        var job = new RecommendationSnapshot.EligibleJob(
                new UUID(0, 1),
                "job-v1",
                List.of(
                        new RecommendationSnapshot.SkillRequirement(
                                "Java", 70, RecommendationSnapshot.RequirementType.REQUIRED),
                        new RecommendationSnapshot.SkillRequirement(
                                "Docker", 60, RecommendationSnapshot.RequirementType.PREFERRED)));

        String fallback = RecommendationExplanation.fallback(
                RecommendationExplanation.createPrompt(snapshot, job));

        assertThat(fallback).contains("Java", "Docker");
        assertThat(fallback.split("\\s+")).hasSizeLessThanOrEqualTo(60);
    }
}
