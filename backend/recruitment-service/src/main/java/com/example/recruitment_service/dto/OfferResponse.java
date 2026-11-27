package com.example.recruitment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.recruitment_service.entity.Offer;
import com.example.recruitment_service.enums.OfferStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;

public record OfferResponse(
        UUID id,
        UUID applicationId,
        UUID jobId,
        UUID companyId,
        String companyName,
        UUID candidateId,
        String candidateEmail,
        String position,
        BigDecimal salary,
        LocalDate startDate,
        String workLocation,
        String note,
        Instant responseDeadline,
        OfferStatus status,
        RecruitmentStatus applicationStatus,
        Instant respondedAt,
        String declineReason,
        Instant confirmedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static OfferResponse from(Offer offer, Instant confirmedAt) {
        var r = offer.getRecruitment();
        return new OfferResponse(
                offer.getId(),
                r != null ? r.getId() : null,
                r != null ? r.getJob_id() : null,
                r != null ? r.getCompany_id() : null,
                offer.getCompanyName(),
                r != null ? r.getUser_id() : null,
                r != null ? r.getCandidate_email() : null,
                offer.getPosition(),
                offer.getSalary(),
                offer.getStartDate(),
                offer.getWorkLocation(),
                offer.getNote(),
                offer.getResponseDeadline(),
                offer.getStatus(),
                r != null ? r.getStatus() : null,
                offer.getRespondedAt(),
                offer.getDeclineReason(),
                confirmedAt,
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }

    public static OfferResponse from(Offer offer) {
        return from(offer, null);
    }
}
