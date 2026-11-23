package com.example.jobservice.match.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MatchScoreRepository extends JpaRepository<MatchScore, MatchScoreId> {

    @Modifying
    @Query("update MatchScore m set m.stale = true where m.jobId = :jobId")
    int markStaleByJobId(@Param("jobId") UUID jobId);
}

