package com.example.authservice.repository;

import com.example.authservice.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
    List<LoginHistory> findByEmailOrderByCreatedAtDesc(String email);
    Page<LoginHistory> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);
    List<LoginHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<LoginHistory> findTop50ByOrderByCreatedAtDesc();
}
