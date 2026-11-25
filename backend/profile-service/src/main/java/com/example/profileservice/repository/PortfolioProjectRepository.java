package com.example.profileservice.repository;

import com.example.profileservice.entity.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, UUID> {

    List<PortfolioProject> findByUserIdOrderBySortOrderAsc(UUID userId);

    List<PortfolioProject> findByUserIdAndVisibleTrueOrderBySortOrderAsc(UUID userId);

    @Query("SELECT p FROM PortfolioProject p WHERE p.internshipId = :internshipId")
    Optional<PortfolioProject> findByInternshipId(@Param("internshipId") UUID internshipId);

    @Query("SELECT p FROM PortfolioProject p WHERE p.userId = :userId AND p.source = :source AND p.sourceRef = :sourceRef")
    Optional<PortfolioProject> findByUserIdAndSourceAndSourceRef(
            @Param("userId") UUID userId,
            @Param("source") String source,
            @Param("sourceRef") String sourceRef);

    boolean existsByUserIdAndInternshipId(UUID userId, UUID internshipId);
}