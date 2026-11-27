package com.example.learningservice.repository;

import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    List<Enrollment> findByUserId(UUID userId);

    List<Enrollment> findByUserIdIn(Collection<UUID> userIds);

    List<Enrollment> findByUserIdOrderByEnrolledAtDesc(UUID userId);

    List<Enrollment> findByUserIdAndStatus(UUID userId, EnrollmentStatus status);

    Optional<Enrollment> findByUserIdAndLmsCourseId(UUID userId, String lmsCourseId);

    boolean existsByUserIdAndLmsCourseId(UUID userId, String lmsCourseId);

    List<Enrollment> findByStatusIn(Collection<EnrollmentStatus> statuses);
}

