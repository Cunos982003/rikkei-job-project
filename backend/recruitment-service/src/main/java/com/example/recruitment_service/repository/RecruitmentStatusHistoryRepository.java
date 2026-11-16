package com.example.recruitment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.recruitment_service.entity.RecruitmentStatusHistory;

public interface RecruitmentStatusHistoryRepository extends JpaRepository<RecruitmentStatusHistory, UUID> {
    @Query("select h from RecruitmentStatusHistory h where h.recruitment_id = :recruitmentId order by h.created_at asc")
    List<RecruitmentStatusHistory> findHistory(@Param("recruitmentId") UUID recruitmentId);
}
