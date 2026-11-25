package com.example.jobservice.job.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndStatus(UUID companyId, JobStatus status, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByStatusAndCompanyId(JobStatus status, UUID companyId, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.companyId = :companyId")
    Page<Job> findAllByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.companyId = :companyId")
    Page<Job> findByStatusAndCompanyIdFiltered(@Param("status") JobStatus status, @Param("companyId") UUID companyId, Pageable pageable);

    @Modifying
    @Query("UPDATE Job j SET j.status = 'CLOSED', j.updatedAt = :now WHERE j.status = 'APPROVED' AND j.deadline < :today")
    int closeExpiredJobs(@Param("today") LocalDate today, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE Job j SET j.status = com.example.jobservice.job.domain.JobStatus.CLOSED, j.updatedAt = CURRENT_TIMESTAMP WHERE j.status = com.example.jobservice.job.domain.JobStatus.APPROVED AND j.deadline < :today")
    int closeExpiredJobs(@Param("today") LocalDate today);

    @Query("SELECT j.id FROM Job j WHERE j.status = 'APPROVED' AND j.deadline < :today")
    List<UUID> findExpiredApprovedJobIds(@Param("today") LocalDate today);

    List<Job> findByIdIn(List<UUID> ids);
}
