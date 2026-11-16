package com.example.learningservice.repository;

import com.example.learningservice.model.entity.CandidateTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateTargetRepository extends JpaRepository<CandidateTarget, UUID> {

    Optional<CandidateTarget> findByUserId(UUID userId);
}

