package com.example.profileservice.profile.infrastructure;

import com.example.profileservice.profile.domain.ProfileReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileReviewRepository extends JpaRepository<ProfileReview, UUID> {
    List<ProfileReview> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ProfileReview> findByUserIdInAndActionOrderByCreatedAtDesc(
            Collection<UUID> userIds, ProfileReview.Action action);
}