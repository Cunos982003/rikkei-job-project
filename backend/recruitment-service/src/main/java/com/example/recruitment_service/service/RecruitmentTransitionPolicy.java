package com.example.recruitment_service.service;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.recruitment_service.enums.RecruitmentStatus;

@Component
public class RecruitmentTransitionPolicy {
    private static final Map<RecruitmentStatus, Set<RecruitmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            RecruitmentStatus.SUBMITTED, Set.of(RecruitmentStatus.SCREENING, RecruitmentStatus.REJECTED),
            RecruitmentStatus.SCREENING, Set.of(RecruitmentStatus.INTERVIEW, RecruitmentStatus.REJECTED),
            RecruitmentStatus.INTERVIEW, Set.of(RecruitmentStatus.REJECTED),
            RecruitmentStatus.OFFER, Set.of(RecruitmentStatus.HIRED, RecruitmentStatus.REJECTED));

    public boolean allows(RecruitmentStatus from, RecruitmentStatus to) {
        return from != null && to != null && ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
