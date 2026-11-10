package com.example.ai_career_service.repository;

import com.example.ai_career_service.model.JobRecommendation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRecommendationRepository extends JpaRepository<JobRecommendation, Long> {

    Optional<JobRecommendation> findByUserIdAndJobId(String userId, String jobId);
}
