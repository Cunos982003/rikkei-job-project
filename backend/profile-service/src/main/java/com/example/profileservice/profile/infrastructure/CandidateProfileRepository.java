package com.example.profileservice.profile.infrastructure;

import com.example.profileservice.profile.domain.CandidateProfile;
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
    List<CandidateProfile> findByVisibility(CandidateProfile.Visibility visibility);
    Page<CandidateProfile> findByVisibility(CandidateProfile.Visibility visibility, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CandidateProfile p WHERE p.userId = :userId")
    Optional<CandidateProfile> findByUserIdForUpdate(@Param("userId") UUID userId);
}