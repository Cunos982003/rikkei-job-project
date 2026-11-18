package com.example.recruitment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import com.example.recruitment_service.dto.DeclineOfferRequest;
import com.example.recruitment_service.dto.SendOfferRequest;
import com.example.recruitment_service.entity.EmploymentRecord;
import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.Offer;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.OfferStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.repository.EmploymentRecordRepository;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.OfferRepository;
import com.example.recruitment_service.repository.RecruitmentRepository;
import com.example.recruitment_service.repository.RecruitmentStatusHistoryRepository;
import com.example.recruitment_service.security.UserPrincipal;

class OfferServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID OTHER_CANDIDATE_ID = UUID.randomUUID();
    private static final UUID HR_ID = UUID.randomUUID();
    private static final String AUTH = "Bearer hr-token";

    private OfferRepository offerRepository;
    private EmploymentRecordRepository employmentRecordRepository;
    private RecruitmentRepository recruitmentRepository;
    private InterviewRoundRepository interviewRoundRepository;
    private RecruitmentStatusHistoryRepository historyRepository;
    private JobServiceClient jobServiceClient;
    private ApplicationEventPublisher eventPublisher;
    private OfferService offerService;

    private UserPrincipal hrPrincipal;
    private UserPrincipal candidatePrincipal;
    private UserPrincipal otherCandidatePrincipal;

    @BeforeEach
    void setUp() {
        offerRepository = mock(OfferRepository.class);
        employmentRecordRepository = mock(EmploymentRecordRepository.class);
        recruitmentRepository = mock(RecruitmentRepository.class);
        interviewRoundRepository = mock(InterviewRoundRepository.class);
        historyRepository = mock(RecruitmentStatusHistoryRepository.class);
        jobServiceClient = mock(JobServiceClient.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        offerService = new OfferService(
                offerRepository,
                employmentRecordRepository,
                recruitmentRepository,
                interviewRoundRepository,
                historyRepository,
                jobServiceClient,
                eventPublisher,
                fixedClock);

        hrPrincipal = new UserPrincipal(HR_ID, "HR", COMPANY_ID, "hr1@rikkeijobs.test");
        candidatePrincipal = new UserPrincipal(CANDIDATE_ID, "CANDIDATE", null, "candidate1@rikkeijobs.test");
        otherCandidatePrincipal = new UserPrincipal(OTHER_CANDIDATE_ID, "CANDIDATE", null, "candidate2@rikkeijobs.test");

        when(jobServiceClient.getJob(JOB_ID, AUTH))
                .thenReturn(new JobServiceClient.JobDetails("APPROVED", null, COMPANY_ID, List.of("hr1@rikkeijobs.test"), "Công ty Cổ phần Rikkeisoft"));
        when(recruitmentRepository.save(any(Recruitment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });
        when(employmentRecordRepository.save(any(EmploymentRecord.class))).thenAnswer(inv -> {
            EmploymentRecord r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
    }

    @Test
    void sendOfferRejectsWith409WhenNoPassedRoundExists() {
        Recruitment rec = recruitment(RecruitmentStatus.INTERVIEW);
        when(recruitmentRepository.findById(rec.getId())).thenReturn(Optional.of(rec));
        when(interviewRoundRepository.findByRecruitment_IdOrderByRoundNoAsc(rec.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> offerService.sendOffer(rec.getId(), validSendRequest(), hrPrincipal, AUTH))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void sendOfferRejectsWith409WhenLastRoundIsNotPass() {
        Recruitment rec = recruitment(RecruitmentStatus.INTERVIEW);
        when(recruitmentRepository.findById(rec.getId())).thenReturn(Optional.of(rec));
        when(interviewRoundRepository.findByRecruitment_IdOrderByRoundNoAsc(rec.getId()))
                .thenReturn(List.of(round(rec, 1, InterviewRoundResult.PASS), round(rec, 2, InterviewRoundResult.FAIL)));

        assertThatThrownBy(() -> offerService.sendOffer(rec.getId(), validSendRequest(), hrPrincipal, AUTH))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void sendOfferRejectsWith409WhenApplicationNotInInterview() {
        Recruitment rec = recruitment(RecruitmentStatus.SCREENING);
        when(recruitmentRepository.findById(rec.getId())).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> offerService.sendOffer(rec.getId(), validSendRequest(), hrPrincipal, AUTH))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void sendOfferSucceedsTransitionsToOfferAndPublishesEmailEvent() {
        Recruitment rec = recruitment(RecruitmentStatus.INTERVIEW);
        when(recruitmentRepository.findById(rec.getId())).thenReturn(Optional.of(rec));
        when(interviewRoundRepository.findByRecruitment_IdOrderByRoundNoAsc(rec.getId()))
                .thenReturn(List.of(round(rec, 1, InterviewRoundResult.PASS)));
        when(offerRepository.existsByRecruitment_Id(rec.getId())).thenReturn(false);

        var response = offerService.sendOffer(rec.getId(), validSendRequest(), hrPrincipal, AUTH);

        assertThat(response.status()).isEqualTo(OfferStatus.SENT);
        assertThat(response.applicationStatus()).isEqualTo(RecruitmentStatus.OFFER);
        assertThat(response.companyName()).isEqualTo("Công ty Cổ phần Rikkeisoft");
        assertThat(rec.getStatus()).isEqualTo(RecruitmentStatus.OFFER);

        verify(historyRepository).save(any(RecruitmentStatusHistory.class));
        verify(eventPublisher).publishEvent(any(OfferSentEvent.class));
    }

    @Test
    void acceptOfferTransitionsToHiredAndCreatesEmploymentRecord() {
        Recruitment rec = recruitment(RecruitmentStatus.OFFER);
        Offer offer = sentOffer(rec, FIXED_NOW.plus(2, ChronoUnit.DAYS));
        when(offerRepository.findByIdAndCandidateId(offer.getId(), CANDIDATE_ID)).thenReturn(Optional.of(offer));
        when(employmentRecordRepository.existsByApplicationId(rec.getId())).thenReturn(false);

        var response = offerService.acceptOffer(offer.getId(), candidatePrincipal);

        assertThat(response.status()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(response.respondedAt()).isEqualTo(FIXED_NOW);
        assertThat(rec.getStatus()).isEqualTo(RecruitmentStatus.HIRED);

        ArgumentCaptor<EmploymentRecord> empCaptor = ArgumentCaptor.forClass(EmploymentRecord.class);
        verify(employmentRecordRepository).save(empCaptor.capture());
        EmploymentRecord created = empCaptor.getValue();
        assertThat(created.getUserId()).isEqualTo(CANDIDATE_ID);
        assertThat(created.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(created.getJobId()).isEqualTo(JOB_ID);
        assertThat(created.getPosition()).isEqualTo("Java Backend Developer");
        assertThat(created.getConfirmedAt()).isNull();
    }

    @Test
    void acceptOfferRejectsExpiredOfferEvenBeforeHourlyJobRuns() {
        Recruitment rec = recruitment(RecruitmentStatus.OFFER);
        Offer offer = sentOffer(rec, FIXED_NOW.minus(1, ChronoUnit.MINUTES));
        when(offerRepository.findByIdAndCandidateId(offer.getId(), CANDIDATE_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.acceptOffer(offer.getId(), candidatePrincipal))
                .isInstanceOf(ApiException.class)
                .extracting("status", "code")
                .containsExactly(HttpStatus.CONFLICT, "OFFER_EXPIRED");

        verify(employmentRecordRepository, never()).save(any());
    }

    @Test
    void anotherCandidateOpeningOrAcceptingOfferGets404() {
        UUID offerId = UUID.randomUUID();
        when(offerRepository.findByIdAndCandidateId(offerId, OTHER_CANDIDATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.getCandidateOffer(offerId, otherCandidatePrincipal))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> offerService.acceptOffer(offerId, otherCandidatePrincipal))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void declineOfferRequiresReasonAndMarksApplicationRejected() {
        Recruitment rec = recruitment(RecruitmentStatus.OFFER);
        Offer offer = sentOffer(rec, FIXED_NOW.plus(2, ChronoUnit.DAYS));
        when(offerRepository.findByIdAndCandidateId(offer.getId(), CANDIDATE_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.declineOffer(offer.getId(), new DeclineOfferRequest("   "), candidatePrincipal))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        var response = offerService.declineOffer(offer.getId(), new DeclineOfferRequest("Đã nhận offer gần nhà hơn"), candidatePrincipal);
        assertThat(response.status()).isEqualTo(OfferStatus.DECLINED);
        assertThat(response.declineReason()).isEqualTo("Đã nhận offer gần nhà hơn");
        assertThat(rec.getStatus()).isEqualTo(RecruitmentStatus.REJECTED);
    }

    @Test
    void confirmOnboardRecordsConfirmedAtOnlyOnceWhenAccepted() {
        Recruitment rec = recruitment(RecruitmentStatus.HIRED);
        Offer offer = sentOffer(rec, FIXED_NOW.plus(2, ChronoUnit.DAYS));
        offer.setStatus(OfferStatus.ACCEPTED);

        EmploymentRecord emp = new EmploymentRecord();
        emp.setId(UUID.randomUUID());
        emp.setUserId(CANDIDATE_ID);
        emp.setCompanyId(COMPANY_ID);
        emp.setCompanyName("Công ty Cổ phần Rikkeisoft");
        emp.setJobId(JOB_ID);
        emp.setApplicationId(rec.getId());
        emp.setPosition("Java Backend Developer");
        emp.setStartDate(LocalDate.of(2026, 12, 1));

        when(recruitmentRepository.findById(rec.getId())).thenReturn(Optional.of(rec));
        when(offerRepository.findByRecruitment_Id(rec.getId())).thenReturn(Optional.of(offer));
        when(employmentRecordRepository.findByApplicationId(rec.getId())).thenReturn(Optional.of(emp));

        var result = offerService.confirmOnboard(rec.getId(), hrPrincipal, AUTH);
        assertThat(result.confirmedAt()).isEqualTo(FIXED_NOW);

        assertThatThrownBy(() -> offerService.confirmOnboard(rec.getId(), hrPrincipal, AUTH))
                .isInstanceOf(ApiException.class)
                .extracting("status", "code")
                .containsExactly(HttpStatus.CONFLICT, "ALREADY_CONFIRMED");
    }

    @Test
    void expireOverdueOffersMarksOffersExpiredAndApplicationsRejected() {
        Recruitment rec = recruitment(RecruitmentStatus.OFFER);
        Offer offer = sentOffer(rec, FIXED_NOW.minus(1, ChronoUnit.HOURS));
        when(offerRepository.findOverdueOffers(OfferStatus.SENT, FIXED_NOW)).thenReturn(List.of(offer));

        int count = offerService.expireOverdueOffers();

        assertThat(count).isEqualTo(1);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.EXPIRED);
        assertThat(rec.getStatus()).isEqualTo(RecruitmentStatus.REJECTED);
        verify(historyRepository).save(any(RecruitmentStatusHistory.class));
        verify(eventPublisher).publishEvent(any(RecruitmentStatusChangedEvent.class));
    }

    private Recruitment recruitment(RecruitmentStatus status) {
        Recruitment r = new Recruitment();
        r.setId(UUID.randomUUID());
        r.setJob_id(JOB_ID);
        r.setCompany_id(COMPANY_ID);
        r.setUser_id(CANDIDATE_ID);
        r.setCandidate_email("candidate1@rikkeijobs.test");
        r.setStatus(status);
        return r;
    }

    private InterviewRound round(Recruitment r, int roundNo, InterviewRoundResult result) {
        InterviewRound round = new InterviewRound();
        round.setId(UUID.randomUUID());
        round.setRecruitment(r);
        round.setRoundNo(roundNo);
        round.setResult(result);
        round.setScore(result == InterviewRoundResult.PASS ? 85 : 45);
        return round;
    }

    private Offer sentOffer(Recruitment r, Instant deadline) {
        Offer offer = new Offer();
        offer.setId(UUID.randomUUID());
        offer.setRecruitment(r);
        offer.setCompanyName("Công ty Cổ phần Rikkeisoft");
        offer.setPosition("Java Backend Developer");
        offer.setSalary(new BigDecimal("18000000"));
        offer.setStartDate(LocalDate.of(2026, 12, 1));
        offer.setWorkLocation("Hà Nội");
        offer.setResponseDeadline(deadline);
        offer.setStatus(OfferStatus.SENT);
        return offer;
    }

    private SendOfferRequest validSendRequest() {
        return new SendOfferRequest(
                "Java Backend Developer",
                new BigDecimal("18000000"),
                LocalDate.of(2026, 12, 1),
                "Tòa nhà Handico, Hà Nội",
                FIXED_NOW.plus(3, ChronoUnit.DAYS),
                "Chào mừng gia nhập đội ngũ!",
                null);
    }
}
