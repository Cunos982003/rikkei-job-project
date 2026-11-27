package com.example.profileservice.repository;

import com.example.profileservice.entity.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CVRepository extends JpaRepository<CV, UUID> {

    List<CV> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<CV> findByPublicToken(String publicToken);

    long countByUserId(UUID userId);

    boolean existsByPublicToken(String publicToken);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CV c WHERE c.userId = :userId AND c.publicToken = :publicToken")
    boolean existsByUserIdAndPublicToken(@Param("userId") UUID userId, @Param("publicToken") String publicToken);

    @Query("SELECT DISTINCT c FROM CV c LEFT JOIN FETCH c.sections s WHERE c.userId = :userId ORDER BY c.createdAt DESC, s.sortOrder ASC")
    List<CV> findByUserIdWithSectionsOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT c FROM CV c LEFT JOIN FETCH c.sections s WHERE c.publicToken = :publicToken")
    Optional<CV> findByPublicTokenWithSections(@Param("publicToken") String publicToken);

    @Query("SELECT DISTINCT c FROM CV c LEFT JOIN FETCH c.sections s WHERE c.id = :cvId")
    Optional<CV> findByIdWithSections(@Param("cvId") UUID cvId);
}