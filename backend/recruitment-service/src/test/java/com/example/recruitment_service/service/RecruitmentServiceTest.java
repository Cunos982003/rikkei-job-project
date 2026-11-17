package com.example.recruitment_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import com.example.recruitment_service.dto.ApplyRequest;
import com.example.recruitment_service.dto.CreateInterviewRoundRequest;
import com.example.recruitment_service.dto.TransitionRequest;
import com.example.recruitment_service.dto.UpdateInterviewRoundRequest;
import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.CvServiceClient;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.RecruitmentRepository;
import com.example.recruitment_service.repository.RecruitmentStatusHistoryRepository;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.storage.CvSnapshotStorage;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CV_ID = UUID.randomUUID();
    private static final UUID RECRUITMENT_ID = UUID.randomUUID();
    private static final UUID ROUND_ID = UUID.randomUUID();
    private static final UUID HR_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String CANDIDATE_EMAIL = "candidate@example.test";

    @Mock
    private RecruitmentRepository recruitmentRepository;
    @Mock
    private RecruitmentStatusHistoryRepository historyRepository;
    @Mock
    private InterviewRoundRepository interviewRoundRepository;
    @Mock
    private RecruitmentTransitionPolicy transitionPolicy;
    @Mock
    private JobServiceClient jobServiceClient;
    @Mock
    private CvServiceClient cvServiceClient;
    @Mock
    private CvSnapshotStorage snapshotStorage;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private RecruitmentService recruitmentService;

    private final UserPrincipal candidate = new UserPrincipal(USER_ID, "CANDIDATE", null, CANDIDATE_EMAIL);
    private final UserPrincipal candidateWithoutEmail = new UserPrincipal(USER_ID, "CANDIDATE", null, null);
    private final UserPrincipal hr = new UserPrincipal(HR_ID, "HR", COMPANY_ID, "hr@example.test");
    private final ApplyRequest request = new ApplyRequest(CV_ID, "Please consider my recruitment");

    @Test
    void recruitStoresPdfSnapshotAndCreatesInitialHistory() {
        when(jobServiceClient.getJob(eq(JOB_ID), any())).thenReturn(new JobServiceClient.JobDetails(
                "APPROVED", Instant.now().plusSeconds(172800), UUID.randomUUID(), List.of("hr@example.test")));
        when(recruitmentRepository.save(any(Recruitment.class))).thenAnswer(invocation -> {
            Recruitment recruitment = invocation.getArgument(0);
            recruitment.setId(RECRUITMENT_ID);
            return recruitment;
        });
        when(recruitmentRepository.existsActiveByJobAndUser(JOB_ID, USER_ID, RecruitmentStatus.WITHDRAWN))
                .thenReturn(false);
        when(cvServiceClient.downloadPdf(CV_ID, "Bearer token")).thenReturn(new byte[] {'%', 'P', 'D', 'F', '-'});
        when(snapshotStorage.store(eq(RECRUITMENT_ID), any())).thenReturn("/recruitments/" + RECRUITMENT_ID + "/cv-snapshot");

        var result = recruitmentService.recruit(JOB_ID, request, candidate, "Bearer token");

        assertEquals(RecruitmentStatus.SUBMITTED, result.status());
        assertEquals("/recruitments/" + RECRUITMENT_ID + "/cv-snapshot", result.cvSnapshotUrl());
        verify(historyRepository).save(argThat(history ->
                history.getFrom_status() == RecruitmentStatus.SUBMITTED
                        && history.getTo_status() == RecruitmentStatus.SUBMITTED
                        && history.getActor_id().equals(USER_ID)));
        verify(recruitmentRepository, times(2)).save(argThat(saved ->
                CANDIDATE_EMAIL.equals(saved.getCandidate_email())));
        verify(eventPublisher).publishEvent(any(RecruitmentSubmittedEvent.class));
    }

    @Test
    void recruitRejectsSecondActiveRecruitmentForSameJob() {
        when(jobServiceClient.getJob(eq(JOB_ID), any())).thenReturn(new JobServiceClient.JobDetails(
                "APPROVED", Instant.now().plusSeconds(172800), UUID.randomUUID(), List.of()));
        when(recruitmentRepository.existsActiveByJobAndUser(JOB_ID, USER_ID, RecruitmentStatus.WITHDRAWN))
                .thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.recruit(JOB_ID, request, candidate, ""));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(cvServiceClient, never()).downloadPdf(any(), any());
        verify(recruitmentRepository, never()).save(any());
    }

    @Test
    void recruitAllowsNewRecruitmentAfterPreviousOneWasWithdrawn() {
        when(jobServiceClient.getJob(eq(JOB_ID), any())).thenReturn(new JobServiceClient.JobDetails(
                "APPROVED", Instant.now().plusSeconds(172800), UUID.randomUUID(), List.of()));
        when(recruitmentRepository.existsActiveByJobAndUser(JOB_ID, USER_ID, RecruitmentStatus.WITHDRAWN))
                .thenReturn(false);
        when(recruitmentRepository.save(any(Recruitment.class))).thenAnswer(invocation -> {
            Recruitment recruitment = invocation.getArgument(0);
            recruitment.setId(RECRUITMENT_ID);
            return recruitment;
        });
        when(cvServiceClient.downloadPdf(CV_ID, "")).thenReturn(new byte[] {'%', 'P', 'D', 'F', '-'});
        when(snapshotStorage.store(eq(RECRUITMENT_ID), any())).thenReturn("/recruitments/" + RECRUITMENT_ID + "/cv-snapshot");

        var result = recruitmentService.recruit(JOB_ID, request, candidate, "");

        assertEquals(RecruitmentStatus.SUBMITTED, result.status());
        verify(recruitmentRepository).existsActiveByJobAndUser(JOB_ID, USER_ID, RecruitmentStatus.WITHDRAWN);
        verify(historyRepository).save(any(RecruitmentStatusHistory.class));
    }

    @Test
    void recruitRejectsUnapprovedJob() {
        when(jobServiceClient.getJob(eq(JOB_ID), any())).thenReturn(new JobServiceClient.JobDetails(
                "PENDING", Instant.now().plusSeconds(172800), UUID.randomUUID(), List.of()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.recruit(JOB_ID, request, candidate, "Bearer token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(cvServiceClient, never()).downloadPdf(any(), any());
        verify(recruitmentRepository, never()).save(any());
    }

    @Test
    void recruitRejectsExpiredJob() {
        when(jobServiceClient.getJob(eq(JOB_ID), any())).thenReturn(new JobServiceClient.JobDetails(
                "APPROVED", Instant.now().minusSeconds(1), UUID.randomUUID(), List.of()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.recruit(JOB_ID, request, candidate, "Bearer token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(cvServiceClient, never()).downloadPdf(any(), any());
    }

    @Test
    void withdrawingOfferIsRejectedWithoutChangingStatusOrHistory() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.OFFER);
        when(recruitmentRepository.findForUserById(RECRUITMENT_ID, USER_ID))
                .thenReturn(java.util.Optional.of(recruitment));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.withdraw(RECRUITMENT_ID, "reason", candidate));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(RecruitmentStatus.OFFER, recruitment.getStatus());
        verify(recruitmentRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void withdrawRecordsEveryStatusTransition() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.INTERVIEW);
        recruitment.setCandidate_email(CANDIDATE_EMAIL);
        when(recruitmentRepository.findForUserById(RECRUITMENT_ID, USER_ID))
                .thenReturn(java.util.Optional.of(recruitment));
        when(recruitmentRepository.save(recruitment)).thenReturn(recruitment);

        var result = recruitmentService.withdraw(RECRUITMENT_ID, " candidate request ", candidate);

        assertEquals(RecruitmentStatus.WITHDRAWN, result.status());
        verify(historyRepository).save(argThat(history ->
                history.getFrom_status() == RecruitmentStatus.INTERVIEW
                        && history.getTo_status() == RecruitmentStatus.WITHDRAWN
                        && "candidate request".equals(history.getNote())
                        && history.getActor_id().equals(USER_ID)));
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof RecruitmentStatusChangedEvent changed
                        && changed.recruitmentId().equals(RECRUITMENT_ID)
                        && changed.candidateEmail().equals(CANDIDATE_EMAIL)
                        && changed.fromStatus() == RecruitmentStatus.INTERVIEW
                        && changed.toStatus() == RecruitmentStatus.WITHDRAWN));
    }

    @Test
    void withdrawRejectsLegacyRecruitmentWithoutCandidateEmail() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.INTERVIEW);
        when(recruitmentRepository.findForUserById(RECRUITMENT_ID, USER_ID))
                .thenReturn(java.util.Optional.of(recruitment));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.withdraw(RECRUITMENT_ID, "candidate request", candidate));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(RecruitmentStatus.INTERVIEW, recruitment.getStatus());
        verify(recruitmentRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cvSnapshotReadUsesStoredSubmissionCopyInsteadOfCurrentCv() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.SUBMITTED);
        byte[] submittedPdf = new byte[] {'%', 'P', 'D', 'F', '-', 1};
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(snapshotStorage.read(RECRUITMENT_ID)).thenReturn(submittedPdf);

        byte[] result = recruitmentService.getCvSnapshot(RECRUITMENT_ID, candidate, "");

        assertArrayEquals(submittedPdf, result);
        verify(snapshotStorage).read(RECRUITMENT_ID);
        verify(cvServiceClient, never()).downloadPdf(any(), any());
    }

    @Test
    void recruitRejectsCandidateWithoutEmailBeforeCallingJobService() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.recruit(JOB_ID, request, candidateWithoutEmail, "Bearer token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(jobServiceClient, never()).getJob(any(), any());
        verify(recruitmentRepository, never()).save(any());
    }

    @Test
    void transitionPersistsStatusHistoryAndPublishesCandidateEvent() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.SUBMITTED);
        recruitment.setCandidate_email(CANDIDATE_EMAIL);
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(jobServiceClient.getJob(JOB_ID, "Bearer token")).thenReturn(jobDetails());
        when(transitionPolicy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.SCREENING)).thenReturn(true);
        when(recruitmentRepository.save(recruitment)).thenReturn(recruitment);

        var result = recruitmentService.transition(
                RECRUITMENT_ID, new TransitionRequest(RecruitmentStatus.SCREENING, "Resume review complete"), hr, "Bearer token");

        assertEquals(RecruitmentStatus.SCREENING, result.status());
        verify(historyRepository).save(argThat(history ->
                history.getRecruitment_id().equals(RECRUITMENT_ID)
                        && history.getFrom_status() == RecruitmentStatus.SUBMITTED
                        && history.getTo_status() == RecruitmentStatus.SCREENING
                        && history.getActor_id().equals(HR_ID)
                        && "Resume review complete".equals(history.getNote())));
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof RecruitmentStatusChangedEvent changed
                        && changed.recruitmentId().equals(RECRUITMENT_ID)
                        && changed.candidateEmail().equals(CANDIDATE_EMAIL)
                        && changed.fromStatus() == RecruitmentStatus.SUBMITTED
                        && changed.toStatus() == RecruitmentStatus.SCREENING));
    }

    @Test
    void transitionRejectsInvalidEdgeWithoutPersistence() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.SUBMITTED);
        recruitment.setCandidate_email(CANDIDATE_EMAIL);
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(jobServiceClient.getJob(JOB_ID, "Bearer token")).thenReturn(jobDetails());
        when(transitionPolicy.allows(RecruitmentStatus.SUBMITTED, RecruitmentStatus.HIRED)).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.transition(
                        RECRUITMENT_ID, new TransitionRequest(RecruitmentStatus.HIRED, "No"), hr, "Bearer token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(RecruitmentStatus.SUBMITTED, recruitment.getStatus());
        verify(recruitmentRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void assignsSequentialRoundNumbers() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.INTERVIEW);
        recruitment.setCandidate_email(CANDIDATE_EMAIL);
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(jobServiceClient.getJob(JOB_ID, "Bearer token")).thenReturn(jobDetails());
        when(interviewRoundRepository.findByRecruitment_IdOrderByRoundNoAsc(RECRUITMENT_ID))
                .thenReturn(List.of(round(recruitment, 1), round(recruitment, 2)));
        when(interviewRoundRepository.save(any(InterviewRound.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = recruitmentService.createRound(
                RECRUITMENT_ID, new CreateInterviewRoundRequest(), hr, "Bearer token");

        assertEquals(3, result.getRoundNo());
        assertNull(result.getScheduledAt());
        verify(interviewRoundRepository).save(argThat(round ->
                round.getRoundNo() == 3
                        && round.getResult() == InterviewRoundResult.PENDING
                        && round.getScheduledAt() == null));
    }

    @Test
    void rejectsRoundScoreOutsideZeroToHundred() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.INTERVIEW);
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(jobServiceClient.getJob(JOB_ID, "Bearer token")).thenReturn(jobDetails());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.updateRound(
                        RECRUITMENT_ID,
                        ROUND_ID,
                        new UpdateInterviewRoundRequest(InterviewRoundResult.PASS, 101),
                        hr,
                        "Bearer token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(interviewRoundRepository, never()).save(any());
    }

    @Test
    void rejectsRoundWriteOutsideInterview() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.SCREENING);
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(jobServiceClient.getJob(JOB_ID, "Bearer token")).thenReturn(jobDetails());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.createRound(
                        RECRUITMENT_ID, new CreateInterviewRoundRequest(), hr, "Bearer token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(interviewRoundRepository, never()).save(any());
    }

    @Test
    void rejectsLegacyTransitionWithoutCandidateEmail() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.SUBMITTED);
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(jobServiceClient.getJob(JOB_ID, "Bearer token")).thenReturn(jobDetails());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.transition(
                        RECRUITMENT_ID, new TransitionRequest(RecruitmentStatus.SCREENING, "advance"), hr, "Bearer token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(recruitmentRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private JobServiceClient.JobDetails jobDetails() {
        return new JobServiceClient.JobDetails("APPROVED", Instant.now().plusSeconds(3600), COMPANY_ID, List.of());
    }

    private InterviewRound round(Recruitment recruitment, int number) {
        InterviewRound round = new InterviewRound();
        round.setId(UUID.randomUUID());
        round.setRecruitment(recruitment);
        round.setRoundNo(number);
        round.setResult(InterviewRoundResult.PENDING);
        return round;
    }

    private Recruitment recruitmentWithStatus(RecruitmentStatus status) {
        Recruitment recruitment = new Recruitment();
        recruitment.setId(RECRUITMENT_ID);
        recruitment.setJob_id(JOB_ID);
        recruitment.setUser_id(USER_ID);
        recruitment.setStatus(status);
        return recruitment;
    }
}
