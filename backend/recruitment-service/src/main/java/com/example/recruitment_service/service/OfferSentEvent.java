package com.example.recruitment_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OfferSentEvent(
        UUID offerId,
        UUID applicationId,
        String candidateEmail,
        String companyName,
        String position,
        BigDecimal salary,
        LocalDate startDate,
        String workLocation,
        Instant responseDeadline
) {
}
