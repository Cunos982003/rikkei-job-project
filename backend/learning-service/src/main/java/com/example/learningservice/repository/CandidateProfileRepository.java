package com.example.learningservice.repository;

import com.example.learningservice.model.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    List<CandidateProfile> findByClassCode(String classCode);

    List<CandidateProfile> findByCourseNameContainingIgnoreCase(String courseName);

    List<CandidateProfile> findByClassCodeAndCourseNameContainingIgnoreCase(String classCode, String courseName);
}
