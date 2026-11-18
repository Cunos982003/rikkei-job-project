package com.example.recruitment_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.recruitment_service.enums.RecruitmentStatus;

class RecruitmentTransitionPolicyTest {

    private final RecruitmentTransitionPolicy policy = new RecruitmentTransitionPolicy();

    @Test
    void allowsEveryConfiguredTransition() {
        assertThat(policy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.SCREENING)).isTrue();
        assertThat(policy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.REJECTED)).isTrue();
        assertThat(policy.allows(RecruitmentStatus.SCREENING, RecruitmentStatus.INTERVIEW)).isTrue();
        assertThat(policy.allows(RecruitmentStatus.SCREENING, RecruitmentStatus.REJECTED)).isTrue();
        assertThat(policy.allows(RecruitmentStatus.INTERVIEW, RecruitmentStatus.REJECTED)).isTrue();
        assertThat(policy.allows(RecruitmentStatus.OFFER, RecruitmentStatus.HIRED)).isTrue();
        assertThat(policy.allows(RecruitmentStatus.OFFER, RecruitmentStatus.REJECTED)).isTrue();
    }

    @Test
    void rejectsUnconfiguredAndSameStatusTransitions() {
        assertThat(policy.allows(RecruitmentStatus.INTERVIEW, RecruitmentStatus.OFFER)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.SUBMITTED)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.SCREENING, RecruitmentStatus.SUBMITTED)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.HIRED, RecruitmentStatus.SUBMITTED)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.REJECTED, RecruitmentStatus.SUBMITTED)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.WITHDRAWN, RecruitmentStatus.SUBMITTED)).isFalse();
    }

    @Test
    void interviewToOfferIsNotAllowedViaTransitionApi() {
        // INTERVIEW → OFFER only via UT-07 send-offer API, never via transition
        assertThat(policy.allows(RecruitmentStatus.INTERVIEW, RecruitmentStatus.OFFER)).isFalse();
    }

    @Test
    void noDirectJumpsAllowed() {
        assertThat(policy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.OFFER)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.HIRED)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.SCREENING, RecruitmentStatus.OFFER)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.SCREENING, RecruitmentStatus.HIRED)).isFalse();
        assertThat(policy.allows(RecruitmentStatus.INTERVIEW, RecruitmentStatus.HIRED)).isFalse();
    }

    @Test
    void getAllowedTargetsReturnsCorrectSets() {
        assertThat(policy.getAllowedTargets(RecruitmentStatus.SUBMITTED))
                .containsExactlyInAnyOrder(RecruitmentStatus.SCREENING, RecruitmentStatus.REJECTED);
        assertThat(policy.getAllowedTargets(RecruitmentStatus.INTERVIEW))
                .containsExactly(RecruitmentStatus.REJECTED);
        assertThat(policy.getAllowedTargets(RecruitmentStatus.HIRED)).isEmpty();
    }
}
