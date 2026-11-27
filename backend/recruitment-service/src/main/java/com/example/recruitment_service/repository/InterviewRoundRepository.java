package com.example.recruitment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.recruitment_service.entity.InterviewRound;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {
    List<InterviewRound> findByRecruitment_IdOrderByRoundNoAsc(UUID recruitmentId);

    @Query("""
            SELECT r FROM InterviewRound r
            JOIN FETCH r.recruitment rec
            WHERE rec.user_id = :userId
            ORDER BY r.createdAt DESC, r.roundNo DESC
            """)
    List<InterviewRound> findByCandidateUserId(@Param("userId") UUID userId);
}
