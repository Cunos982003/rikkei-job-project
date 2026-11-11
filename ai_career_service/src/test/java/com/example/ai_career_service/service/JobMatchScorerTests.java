package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobMatchScorerTests {

    @Test
    void calculatesEqualWeightMatchAndReportsRequiredAndPreferredGaps() {
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

        assertThat(result.score()).isEqualTo((75.0 + 100 + 0) / 3);
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
    void doesNotFlagMissingPreferredSkillAsMissingRequired() {
        JobMatchScorer.MatchResult result = JobMatchScorer.score(
                List.of(),
                List.of(new RecommendationSnapshot.SkillRequirement(
                        "Docker", 50, RecommendationSnapshot.RequirementType.PREFERRED)));

        assertThat(result.score()).isZero();
        assertThat(result.missingRequiredSkills()).isEmpty();
        assertThat(result.skillGaps().get(0).gap()).isEqualTo(50);
    }

    @Test
    void returnsZeroWhenJobHasNoSkillRequirements() {
        assertThat(JobMatchScorer.score(List.of(), List.of()).score()).isZero();
    }
}
