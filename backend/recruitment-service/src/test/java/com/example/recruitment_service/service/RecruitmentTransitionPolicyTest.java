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
}
