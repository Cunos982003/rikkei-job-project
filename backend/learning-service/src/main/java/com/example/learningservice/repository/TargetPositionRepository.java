package com.example.learningservice.repository;

import com.example.learningservice.model.entity.TargetPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TargetPositionRepository extends JpaRepository<TargetPosition, UUID> {

    Optional<TargetPosition> findByCode(String code);

    @EntityGraph(attributePaths = {"skills"})
    List<TargetPosition> findAll();

    @EntityGraph(attributePaths = {"skills"})
    Optional<TargetPosition> findWithSkillsById(UUID id);
}

