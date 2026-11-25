package com.example.profileservice.repository;

import com.example.profileservice.entity.ManualOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManualOverrideRepository extends JpaRepository<ManualOverride, UUID> {

    List<ManualOverride> findByRecordId(UUID recordId);

    Optional<ManualOverride> findByRecordIdAndField(UUID recordId, String field);

    List<ManualOverride> findByRecordIdIn(List<UUID> recordIds);
}
