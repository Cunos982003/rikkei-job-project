package com.example.recruitment_service.repository;

import com.example.recruitment_service.entity.EvaluationLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvaluationLinkRepository extends JpaRepository<EvaluationLink, UUID> {
    Optional<EvaluationLink> findByTokenHash(String tokenHash);
}