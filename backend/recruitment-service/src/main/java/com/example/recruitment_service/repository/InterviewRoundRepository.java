package com.example.recruitment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.recruitment_service.entity.InterviewRound;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {
    List<InterviewRound> findByRecruitment_IdOrderByRoundNoAsc(UUID recruitmentId);
}
