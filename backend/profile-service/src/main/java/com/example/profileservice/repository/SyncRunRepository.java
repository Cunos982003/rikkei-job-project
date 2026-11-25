package com.example.profileservice.repository;

import com.example.profileservice.entity.SyncRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncRunRepository extends JpaRepository<SyncRun, UUID> {

    Page<SyncRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    Page<SyncRun> findByStatusOrderByStartedAtDesc(String status, Pageable pageable);

    Page<SyncRun> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Page<SyncRun> findByStatusAndUserIdOrderByStartedAtDesc(String status, UUID userId, Pageable pageable);

    Optional<SyncRun> findTopByUserIdOrderByStartedAtDesc(UUID userId);
}
