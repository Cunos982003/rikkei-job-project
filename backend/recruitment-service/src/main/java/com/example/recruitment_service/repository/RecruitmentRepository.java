package com.example.recruitment_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.RecruitmentStatus;

public interface RecruitmentRepository extends JpaRepository<Recruitment, UUID> {
    @Query("select r from Recruitment r where r.user_id = :userId order by r.created_at desc")
    List<Recruitment> findForUser(@Param("userId") UUID userId);

    @Query("select r from Recruitment r where r.user_id = :userId and r.status = :status order by r.created_at desc")
    List<Recruitment> findForUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") RecruitmentStatus status);

    @Query("select r from Recruitment r where r.id = :id and r.user_id = :userId")
    Optional<Recruitment> findForUserById(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select case when count(r) > 0 then true else false end from Recruitment r "
            + "where r.job_id = :jobId and r.user_id = :userId and r.status <> :withdrawn")
    boolean existsActiveByJobAndUser(
            @Param("jobId") UUID jobId,
            @Param("userId") UUID userId,
            @Param("withdrawn") RecruitmentStatus withdrawn);

    @Query(
            value = "select r from Recruitment r "
                    + "where (:companyId is null or r.company_id = :companyId) "
                    + "and (:jobId is null or r.job_id = :jobId) "
                    + "and (:status is null or r.status = :status)",
            countQuery = "select count(r) from Recruitment r "
                    + "where (:companyId is null or r.company_id = :companyId) "
                    + "and (:jobId is null or r.job_id = :jobId) "
                    + "and (:status is null or r.status = :status)")
    Page<Recruitment> findForStaff(
            @Param("companyId") UUID companyId,
            @Param("jobId") UUID jobId,
            @Param("status") RecruitmentStatus status,
            Pageable pageable);
}
