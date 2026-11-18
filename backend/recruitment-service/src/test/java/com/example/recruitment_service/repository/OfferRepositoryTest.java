package com.example.recruitment_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.recruitment_service.entity.EmploymentRecord;
import com.example.recruitment_service.entity.Offer;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.OfferStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OfferRepositoryTest {

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private EmploymentRecordRepository employmentRecordRepository;

    @Test
    void findsOfferOnlyForOwningCandidate() {
        UUID ownerId = UUID.randomUUID();
        UUID otherCandidateId = UUID.randomUUID();
        Recruitment recruitment = createRecruitment(ownerId, RecruitmentStatus.OFFER);

        Offer offer = createOffer(recruitment, OfferStatus.SENT, Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(offerRepository.findByIdAndCandidateId(offer.getId(), ownerId)).isPresent();
        assertThat(offerRepository.findByIdAndCandidateId(offer.getId(), otherCandidateId)).isEmpty();
        assertThat(offerRepository.findForCandidate(ownerId)).hasSize(1);
        assertThat(offerRepository.findForCandidate(otherCandidateId)).isEmpty();
    }

    @Test
    void findsOnlyOverdueSentOffers() {
        Recruitment overdueRec = createRecruitment(UUID.randomUUID(), RecruitmentStatus.OFFER);
        Recruitment futureRec = createRecruitment(UUID.randomUUID(), RecruitmentStatus.OFFER);
        Recruitment acceptedRec = createRecruitment(UUID.randomUUID(), RecruitmentStatus.HIRED);

        Offer overdue = createOffer(overdueRec, OfferStatus.SENT, Instant.now().minus(1, ChronoUnit.HOURS));
        createOffer(futureRec, OfferStatus.SENT, Instant.now().plus(3, ChronoUnit.HOURS));
        createOffer(acceptedRec, OfferStatus.ACCEPTED, Instant.now().minus(2, ChronoUnit.HOURS));

        var results = offerRepository.findOverdueOffers(OfferStatus.SENT, Instant.now());

        assertThat(results).extracting(Offer::getId).containsExactly(overdue.getId());
    }

    @Test
    void persistsAndQueriesEmploymentRecord() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        EmploymentRecord record = new EmploymentRecord();
        record.setUserId(userId);
        record.setCompanyId(companyId);
        record.setCompanyName("Công ty Cổ phần Rikkeisoft");
        record.setJobId(jobId);
        record.setApplicationId(appId);
        record.setPosition("Java Backend Developer");
        record.setStartDate(LocalDate.of(2026, 12, 1));
        employmentRecordRepository.saveAndFlush(record);

        assertThat(employmentRecordRepository.findByApplicationId(appId)).isPresent();
        assertThat(employmentRecordRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(userId))
                .singleElement()
                .extracting(EmploymentRecord::getPosition, EmploymentRecord::getCompanyName)
                .containsExactly("Java Backend Developer", "Công ty Cổ phần Rikkeisoft");
    }

    private Recruitment createRecruitment(UUID userId, RecruitmentStatus status) {
        Recruitment recruitment = new Recruitment();
        recruitment.setJob_id(UUID.randomUUID());
        recruitment.setCompany_id(UUID.randomUUID());
        recruitment.setUser_id(userId);
        recruitment.setCandidate_email("candidate@rikkeijobs.test");
        recruitment.setStatus(status);
        return recruitmentRepository.saveAndFlush(recruitment);
    }

    private Offer createOffer(Recruitment recruitment, OfferStatus status, Instant deadline) {
        Offer offer = new Offer();
        offer.setRecruitment(recruitment);
        offer.setCompanyName("Rikkeisoft");
        offer.setPosition("Java Backend Developer");
        offer.setSalary(new BigDecimal("18000000"));
        offer.setStartDate(LocalDate.of(2026, 12, 1));
        offer.setWorkLocation("Hà Nội");
        offer.setResponseDeadline(deadline);
        offer.setStatus(status);
        return offerRepository.saveAndFlush(offer);
    }
}
