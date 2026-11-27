package com.example.profileservice.repository;

import com.example.profileservice.entity.ExternalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalRecordRepository extends JpaRepository<ExternalRecord, UUID> {

    Optional<ExternalRecord> findBySourceAndSourceKey(String source, String sourceKey);

    List<ExternalRecord> findByUserId(UUID userId);

    List<ExternalRecord> findByUserIdAndSource(UUID userId, String source);

    @Query("SELECT MAX(e.syncedAt) FROM ExternalRecord e WHERE e.userId = :userId")
    Optional<OffsetDateTime> findMaxSyncedAtByUserId(@Param("userId") UUID userId);
}
