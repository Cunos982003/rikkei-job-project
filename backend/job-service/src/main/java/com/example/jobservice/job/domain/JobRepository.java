package com.example.jobservice.job.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndStatus(UUID companyId, JobStatus status, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);
}

