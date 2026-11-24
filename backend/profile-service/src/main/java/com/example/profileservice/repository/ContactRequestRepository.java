package com.example.profileservice.repository;

import com.example.profileservice.entity.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    Optional<ContactRequest> findByCompanyIdAndUserIdAndJobId(UUID companyId, UUID userId, UUID jobId);

    List<ContactRequest> findByCompanyId(UUID companyId);

    List<ContactRequest> findByUserId(UUID userId);

    List<ContactRequest> findByStatus(ContactRequest.ContactRequestStatus status);

    @Query("SELECT cr FROM ContactRequest cr WHERE cr.companyId = :companyId AND cr.userId = :userId AND cr.status = 'APPROVED'")
    boolean existsApprovedRequest(@Param("companyId") UUID companyId, @Param("userId") UUID userId);

    @Query("SELECT cr FROM ContactRequest cr WHERE cr.userId = :userId AND cr.status = 'PENDING' ORDER BY cr.createdAt DESC")
    List<ContactRequest> findPendingByUserId(@Param("userId") UUID userId);
}