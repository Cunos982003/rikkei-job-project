package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobMatchScorerTests {

    @Test
    void calculatesWeightedMatchAndFlagsBelowMinimumForRequiredSkills() {
        List<RecommendationSnapshot.VerifiedSkill> verifiedSkills = List.of(
                new RecommendationSnapshot.VerifiedSkill("Java", 60),
                new RecommendationSnapshot.VerifiedSkill("SQL", 80));
        List<RecommendationSnapshot.SkillRequirement> requirements = List.of(
                new RecommendationSnapshot.SkillRequirement(
                        "Java", 80, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "SQL", 50, RecommendationSnapshot.RequirementType.PREFERRED),
                new RecommendationSnapshot.SkillRequirement(
                        "Docker", 40, RecommendationSnapshot.RequirementType.REQUIRED));

        JobMatchScorer.MatchResult result = JobMatchScorer.score(verifiedSkills, requirements);

        // TB_req = (60/80 + 0/40) / 2 = 0.375; TB_opt = 1.0
        // score = 70 * 0.375 + 30 * 1.0 = 26.25 + 30 = 56.25 -> 56.3 (HALF_UP)
        assertThat(result.score()).isEqualTo(56.3);
        assertThat(result.belowMinimum()).isTrue();
        assertThat(result.missingRequiredSkills()).containsExactly("Java", "Docker");
        assertThat(result.skillGaps()).containsExactly(
                new JobMatchScorer.SkillGap(
                        "Java", 60, 80, 20, RecommendationSnapshot.RequirementType.REQUIRED),
                new JobMatchScorer.SkillGap(
                        "SQL", 80, 50, 0, RecommendationSnapshot.RequirementType.PREFERRED),
                new JobMatchScorer.SkillGap(
                        "Docker", null, 40, 40, RecommendationSnapshot.RequirementType.REQUIRED));
    }

    @Test
    void matchesDn06ExampleAExact50PercentNotBelowMinimum() {
        List<RecommendationSnapshot.VerifiedSkill> verifiedSkills = List.of(
                new RecommendationSnapshot.VerifiedSkill("JAVA_CORE", 70),
                new RecommendationSnapshot.VerifiedSkill("SPRING_BOOT", 30),
                new RecommendationSnapshot.VerifiedSkill("GIT", 50));
        List<RecommendationSnapshot.SkillRequirement> requirements = List.of(
                new RecommendationSnapshot.SkillRequirement(
                        "JAVA_CORE", 70, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "SPRING_BOOT", 60, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "GIT", 50, RecommendationSnapshot.RequirementType.PREFERRED));

        JobMatchScorer.MatchResult result = JobMatchScorer.score(verifiedSkills, requirements);

        assertThat(result.score()).isEqualTo(82.5);
        assertThat(result.belowMinimum()).isFalse();
    }

    @Test
    void matchesDn06ExampleA2Below50PercentTriggersBelowMinimumAndRoundsHalfUp() {
        List<RecommendationSnapshot.VerifiedSkill> verifiedSkills = List.of(
                new RecommendationSnapshot.VerifiedSkill("JAVA_CORE", 70),
                new RecommendationSnapshot.VerifiedSkill("SPRING_BOOT", 29),
                new RecommendationSnapshot.VerifiedSkill("GIT", 50));
        List<RecommendationSnapshot.SkillRequirement> requirements = List.of(
                new RecommendationSnapshot.SkillRequirement(
                        "JAVA_CORE", 70, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "SPRING_BOOT", 60, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "GIT", 50, RecommendationSnapshot.RequirementType.PREFERRED));

        JobMatchScorer.MatchResult result = JobMatchScorer.score(verifiedSkills, requirements);

        assertThat(result.score()).isEqualTo(81.9);
        assertThat(result.belowMinimum()).isTrue();
    }

    @Test
    void usesFullWeightForRequiredSkillsWhenJobHasNoPreferredSkills() {
        List<RecommendationSnapshot.VerifiedSkill> verifiedSkills = List.of(
                new RecommendationSnapshot.VerifiedSkill("JAVA_CORE", 40),
                new RecommendationSnapshot.VerifiedSkill("SQL", 60));
        List<RecommendationSnapshot.SkillRequirement> requirements = List.of(
                new RecommendationSnapshot.SkillRequirement(
                        "JAVA_CORE", 80, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "SQL", 60, RecommendationSnapshot.RequirementType.REQUIRED),
                new RecommendationSnapshot.SkillRequirement(
                        "REACT", 50, RecommendationSnapshot.RequirementType.REQUIRED));

        JobMatchScorer.MatchResult result = JobMatchScorer.score(verifiedSkills, requirements);

        assertThat(result.score()).isEqualTo(50.0);
        assertThat(result.belowMinimum()).isTrue();
    }

    @Test
    void doesNotFlagMissingPreferredSkillAsMissingRequiredOrBelowMinimum() {
        JobMatchScorer.MatchResult result = JobMatchScorer.score(
                List.of(),
                List.of(new RecommendationSnapshot.SkillRequirement(
                        "Docker", 50, RecommendationSnapshot.RequirementType.PREFERRED)));

        assertThat(result.score()).isZero();
        assertThat(result.belowMinimum()).isFalse();
        assertThat(result.missingRequiredSkills()).isEmpty();
        assertThat(result.skillGaps().get(0).gap()).isEqualTo(50);
    }

    @Test
    void treatsMinScoreZeroAsFullRatioAndNotBelowMinimum() {
        JobMatchScorer.MatchResult result = JobMatchScorer.score(
                List.of(),
                List.of(new RecommendationSnapshot.SkillRequirement(
                        "Docker", 0, RecommendationSnapshot.RequirementType.REQUIRED)));

        assertThat(result.score()).isEqualTo(100.0);
        assertThat(result.belowMinimum()).isFalse();
        assertThat(result.skillGaps().get(0).gap()).isZero();
    }

    @Test
    void returnsZeroAndNotBelowMinimumWhenJobHasNoSkillRequirements() {
        JobMatchScorer.MatchResult result = JobMatchScorer.score(List.of(), List.of());

        assertThat(result.score()).isZero();
        assertThat(result.belowMinimum()).isFalse();
    }
}
