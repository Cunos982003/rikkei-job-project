package com.example.profileservice.internship.infrastructure;

import com.example.profileservice.internship.domain.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, UUID> {
    // Đổi Long thành UUID và String
    Optional<Internship> findByUserIdAndBatchId(UUID userId, String batchId);

    List<Internship> findAllByUserIdOrderByStartDateDesc(UUID userId);
}