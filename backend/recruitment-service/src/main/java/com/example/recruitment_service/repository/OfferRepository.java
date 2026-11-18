package com.example.recruitment_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.recruitment_service.entity.Offer;
import com.example.recruitment_service.enums.OfferStatus;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByRecruitment_Id(UUID applicationId);

    boolean existsByRecruitment_Id(UUID applicationId);

    @Query("select o from Offer o join fetch o.recruitment r where r.user_id = :userId order by o.createdAt desc")
    List<Offer> findForCandidate(@Param("userId") UUID userId);

    @Query("select o from Offer o join fetch o.recruitment r where o.id = :offerId and r.user_id = :userId")
    Optional<Offer> findByIdAndCandidateId(@Param("offerId") UUID offerId, @Param("userId") UUID userId);

    @Query("select o from Offer o join fetch o.recruitment r where o.status = :status and o.responseDeadline <= :cutoff")
    List<Offer> findOverdueOffers(@Param("status") OfferStatus status, @Param("cutoff") Instant cutoff);
}
