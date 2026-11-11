package com.example.ai_career_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ai_career_service.model.JobRecommendation;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JobRecommendationRepositoryTests {

    @Autowired
    private JobRecommendationRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndFindsRecommendationsByUuid() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        JobRecommendation recommendation = new JobRecommendation();
        recommendation.setUserId(userId);
        recommendation.setJobId(jobId);
        recommendation.setScore(75);
        recommendation.setExplanation("Match");
        recommendation.setCreatedAt(Instant.now());
        recommendation.setInputHash("hash");

        UUID id = repository.saveAndFlush(recommendation).getId();
        assertThat(id).isNotNull();
        entityManager.clear();

        assertThat(repository.findById(id)).isPresent()
                .get().satisfies(saved -> {
                    assertThat(saved.getUserId()).isEqualTo(userId);
                    assertThat(saved.getJobId()).isEqualTo(jobId);
                });
        assertThat(repository.findByUserIdAndJobId(userId, jobId))
                .get().extracting(JobRecommendation::getId).isEqualTo(id);
    }
}
