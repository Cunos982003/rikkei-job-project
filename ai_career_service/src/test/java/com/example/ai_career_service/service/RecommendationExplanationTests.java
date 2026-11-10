package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
                "job-1",
                "job-v1",
                75,
                List.of(
                        new RecommendationSnapshot.SkillRequirement("Java", 70),
                        new RecommendationSnapshot.SkillRequirement("Kotlin", 60),
                        new RecommendationSnapshot.SkillRequirement("SQL", 50)));

        String prompt = RecommendationExplanation.createPrompt(snapshot, job).toUserPrompt();

        assertThat(prompt).contains("Java", "85", "70", "Kotlin", "40", "60", "SQL", "chưa có");
        assertThat(prompt).doesNotContain("profile-v1", "job-1", "job-v1", "75");
    }

    @Test
    void fallbackIsConciseAndCoversFitAndGaps() {
        var snapshot = new RecommendationSnapshot(
                "profile-v1",
                List.of(new RecommendationSnapshot.VerifiedSkill("Java", 85)),
                List.of());
        var job = new RecommendationSnapshot.EligibleJob(
                "job-1",
                "job-v1",
                75,
                List.of(
                        new RecommendationSnapshot.SkillRequirement("Java", 70),
                        new RecommendationSnapshot.SkillRequirement("Docker", 60)));

        String fallback = RecommendationExplanation.fallback(
                RecommendationExplanation.createPrompt(snapshot, job));

        assertThat(fallback).contains("Java", "Docker");
        assertThat(fallback.split("\\s+")).hasSizeLessThanOrEqualTo(60);
    }
}
