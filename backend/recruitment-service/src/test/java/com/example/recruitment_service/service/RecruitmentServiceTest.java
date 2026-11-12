package com.example.recruitment_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.CvServiceClient;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.repository.RecruitmentRepository;
import com.example.recruitment_service.repository.RecruitmentStatusHistoryRepository;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.storage.CvSnapshotStorage;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CV_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private RecruitmentRepository recruitmentRepository;
    @Mock
    private RecruitmentStatusHistoryRepository historyRepository;
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

    private final UserPrincipal candidate = new UserPrincipal(USER_ID, "CANDIDATE", null);
    private final ApplyRequest request = new ApplyRequest(CV_ID, "Please consider my recruitment");

    @Test
    void recruitStoresPdfSnapshotAndCreatesInitialHistory() {
        when(jobServiceClient.getJob(eq(JOB_ID), any())).thenReturn(new JobServiceClient.JobDetails(
                "APPROVED", Instant.now().plusSeconds(172800), UUID.randomUUID(), List.of("hr@example.test")));
        when(recruitmentRepository.save(any(Recruitment.class))).thenAnswer(invocation -> {
            Recruitment recruitment = invocation.getArgument(0);
            recruitment.setId(APPLICATION_ID);
            return recruitment;
        });
        when(recruitmentRepository.existsActiveByJobAndUser(JOB_ID, USER_ID, RecruitmentStatus.WITHDRAWN))
                .thenReturn(false);
        when(cvServiceClient.downloadPdf(CV_ID, "Bearer token")).thenReturn(new byte[] {'%', 'P', 'D', 'F', '-'});
        when(snapshotStorage.store(eq(APPLICATION_ID), any())).thenReturn("/recruitments/" + APPLICATION_ID + "/cv-snapshot");

        var result = recruitmentService.recruit(JOB_ID, request, candidate, "Bearer token");

        assertEquals(RecruitmentStatus.SUBMITTED, result.status());
        assertEquals("/recruitments/" + APPLICATION_ID + "/cv-snapshot", result.cvSnapshotUrl());
        verify(historyRepository).save(argThat(history ->
                history.getFrom_status() == RecruitmentStatus.SUBMITTED
                        && history.getTo_status() == RecruitmentStatus.SUBMITTED
                        && history.getActor_id().equals(USER_ID)));
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
            recruitment.setId(APPLICATION_ID);
            return recruitment;
        });
        when(cvServiceClient.downloadPdf(CV_ID, "")).thenReturn(new byte[] {'%', 'P', 'D', 'F', '-'});
        when(snapshotStorage.store(eq(APPLICATION_ID), any())).thenReturn("/recruitments/" + APPLICATION_ID + "/cv-snapshot");

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
        when(recruitmentRepository.findForUserById(APPLICATION_ID, USER_ID))
                .thenReturn(java.util.Optional.of(recruitment));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recruitmentService.withdraw(APPLICATION_ID, "reason", candidate));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(RecruitmentStatus.OFFER, recruitment.getStatus());
        verify(recruitmentRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void withdrawRecordsEveryStatusTransition() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.INTERVIEW);
        when(recruitmentRepository.findForUserById(APPLICATION_ID, USER_ID))
                .thenReturn(java.util.Optional.of(recruitment));
        when(recruitmentRepository.save(recruitment)).thenReturn(recruitment);

        var result = recruitmentService.withdraw(APPLICATION_ID, " candidate request ", candidate);

        assertEquals(RecruitmentStatus.WITHDRAWN, result.status());
        verify(historyRepository).save(argThat(history ->
                history.getFrom_status() == RecruitmentStatus.INTERVIEW
                        && history.getTo_status() == RecruitmentStatus.WITHDRAWN
                        && "candidate request".equals(history.getNote())
                        && history.getActor_id().equals(USER_ID)));
    }

    @Test
    void cvSnapshotReadUsesStoredSubmissionCopyInsteadOfCurrentCv() {
        Recruitment recruitment = recruitmentWithStatus(RecruitmentStatus.SUBMITTED);
        byte[] submittedPdf = new byte[] {'%', 'P', 'D', 'F', '-', 1};
        when(recruitmentRepository.findById(APPLICATION_ID)).thenReturn(java.util.Optional.of(recruitment));
        when(snapshotStorage.read(APPLICATION_ID)).thenReturn(submittedPdf);

        byte[] result = recruitmentService.getCvSnapshot(APPLICATION_ID, candidate, "");

        assertArrayEquals(submittedPdf, result);
        verify(snapshotStorage).read(APPLICATION_ID);
        verify(cvServiceClient, never()).downloadPdf(any(), any());
    }

    private Recruitment recruitmentWithStatus(RecruitmentStatus status) {
        Recruitment recruitment = new Recruitment();
        recruitment.setId(APPLICATION_ID);
        recruitment.setJob_id(JOB_ID);
        recruitment.setUser_id(USER_ID);
        recruitment.setStatus(status);
        return recruitment;
    }
}
