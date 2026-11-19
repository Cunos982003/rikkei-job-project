package com.example.profileservice.repository;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.entity.CandidateProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByUserId(UUID userId);
    List<CandidateProfile> findByVisibility(Visibility visibility);
    Page<CandidateProfile> findByVisibility(Visibility visibility, Pageable pageable);

    @Query("SELECT p FROM CandidateProfile p WHERE p.visibility = :visibility " +
           "AND (:q IS NULL OR :q = '' OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "    OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:classCode IS NULL OR :classCode = '' OR p.classCode = :classCode) " +
           "ORDER BY p.updatedAt ASC")
    Page<CandidateProfile> findByVisibilityAndFilters(
            @Param("visibility") Visibility visibility,
            @Param("q") String q,
            @Param("classCode") String classCode,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CandidateProfile p WHERE p.userId = :userId")
    Optional<CandidateProfile> findByUserIdForUpdate(@Param("userId") UUID userId);
}