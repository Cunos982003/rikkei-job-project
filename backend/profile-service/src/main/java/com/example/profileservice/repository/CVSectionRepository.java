package com.example.profileservice.repository;

import com.example.profileservice.entity.CVSection;
import com.example.profileservice.entity.SectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CVSectionRepository extends JpaRepository<CVSection, UUID> {

    List<CVSection> findByCvIdOrderBySortOrderAsc(UUID cvId);

    @Query("SELECT s FROM CVSection s WHERE s.cv.id = :cvId AND s.sectionType = :sectionType")
    Optional<CVSection> findByCvIdAndSectionType(@Param("cvId") UUID cvId, @Param("sectionType") SectionType sectionType);

    @Modifying
    @Transactional
    @Query("DELETE FROM CVSection s WHERE s.cv.id = :cvId AND s.sectionType = :sectionType")
    void deleteByCvIdAndSectionType(@Param("cvId") UUID cvId, @Param("sectionType") SectionType sectionType);

    @Modifying
    @Transactional
    @Query("DELETE FROM CVSection s WHERE s.cv.id = :cvId")
    void deleteByCvId(@Param("cvId") UUID cvId);
}