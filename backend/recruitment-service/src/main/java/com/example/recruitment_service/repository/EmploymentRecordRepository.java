package com.example.recruitment_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.recruitment_service.entity.EmploymentRecord;

public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, UUID> {

    List<EmploymentRecord> findByUserIdOrderByStartDateDescCreatedAtDesc(UUID userId);

    Optional<EmploymentRecord> findByApplicationId(UUID applicationId);

    boolean existsByApplicationId(UUID applicationId);
}
