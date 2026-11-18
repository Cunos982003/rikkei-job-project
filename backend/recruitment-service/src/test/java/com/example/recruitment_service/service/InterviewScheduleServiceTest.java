package com.example.recruitment_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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

import com.example.recruitment_service.dto.InterviewSlotInput;
import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.entity.InterviewSlot;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.InterviewScheduleStatus;
import com.example.recruitment_service.enums.InterviewSlotStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.integration.RikkeiMeetClient;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.InterviewScheduleRepository;
import com.example.recruitment_service.repository.InterviewSlotRepository;
import com.example.recruitment_service.security.UserPrincipal;

class InterviewScheduleServiceTest {
    private static final Instant FIXED_NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CANDIDATE_ID = UUID.fromString("ed854c07-f3eb-48f1-a186-4e7135f54645");
    private static final UUID OTHER_CANDIDATE_ID = UUID.fromString("797c5663-386b-4e96-9112-6d3e414b4449");
    private static final UUID HR_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");
    private static final UUID JOB_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID RECRUITMENT_ID = UUID.fromString("77060000-0000-4000-8000-000000000001");
    private static final UUID ROUND_ID = UUID.fromString("77062000-0000-4000-8000-000000000001");

    private InterviewRoundRepository roundRepository;
    private InterviewSlotRepository slotRepository;
    private InterviewScheduleRepository scheduleRepository;
    private JobServiceClient jobServiceClient;
    private RikkeiMeetClient rikkeiMeetClient;
    private ApplicationEventPublisher eventPublisher;
    private InterviewScheduleService service;

    private UserPrincipal hrActor;
    private UserPrincipal candidateActor;

    @BeforeEach
    void setUp() {
        roundRepository = mock(InterviewRoundRepository.class);
        slotRepository = mock(InterviewSlotRepository.class);
        scheduleRepository = mock(InterviewScheduleRepository.class);
        jobServiceClient = mock(JobServiceClient.class);
        rikkeiMeetClient = mock(RikkeiMeetClient.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

        service = new InterviewScheduleService(
                roundRepository,
                slotRepository,
                scheduleRepository,
                jobServiceClient,
                rikkeiMeetClient,
                eventPublisher,
                clock);

        hrActor = new UserPrincipal(HR_ID, "HR", COMPANY_ID, "hr1@rikkeijobs.test");
        candidateActor = new UserPrincipal(CANDIDATE_ID, "CANDIDATE", null, "candidate1@rikkeijobs.test");
    }

    @Test
    void proposeSlots_rejectsPastSlotWith400() {
        InterviewRound round = sampleRound();
        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));

        List<InterviewSlotInput> pastSlots = List.of(
                new InterviewSlotInput(FIXED_NOW.minus(1, ChronoUnit.HOURS), FIXED_NOW.plus(1, ChronoUnit.HOURS)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.proposeSlots(ROUND_ID, pastSlots, hrActor, "Bearer tok"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("SLOT_IN_PAST", ex.getCode());
    }

    @Test
    void proposeSlots_rejectsDurationShorterThan30MinutesWith400() {
        InterviewRound round = sampleRound();
        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));

        List<InterviewSlotInput> shortSlots = List.of(
                new InterviewSlotInput(
                        FIXED_NOW.plus(2, ChronoUnit.HOURS),
                        FIXED_NOW.plus(2, ChronoUnit.HOURS).plus(29, ChronoUnit.MINUTES)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.proposeSlots(ROUND_ID, shortSlots, hrActor, "Bearer tok"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("SLOT_TOO_SHORT", ex.getCode());
    }

    @Test
    void proposeSlots_rejectsMoreThanFiveSlotsWith400() {
        InterviewRound round = sampleRound();
        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));

        List<InterviewSlotInput> sixSlots = List.of(
                slotInput(2, 3), slotInput(4, 5), slotInput(6, 7),
                slotInput(8, 9), slotInput(10, 11), slotInput(12, 13));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.proposeSlots(ROUND_ID, sixSlots, hrActor, "Bearer tok"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_SLOT_COUNT", ex.getCode());
    }

    @Test
    void proposeSlots_savesValidOpenSlots() {
        InterviewRound round = sampleRound();
        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                ROUND_ID, InterviewScheduleStatus.SCHEDULED)).thenReturn(Optional.empty());
        when(slotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<InterviewSlot> list = inv.getArgument(0);
            for (InterviewSlot s : list) {
                if (s.getId() == null) s.setId(UUID.randomUUID());
            }
            return list;
        });

        var result = service.proposeSlots(ROUND_ID, List.of(slotInput(2, 3), slotInput(5, 6)), hrActor, "Bearer tok");
        assertEquals(2, result.size());
        assertEquals(InterviewSlotStatus.OPEN, result.get(0).status());
        assertEquals(InterviewSlotStatus.OPEN, result.get(1).status());
    }

    @Test
    void pickSlot_createsMeetRoomCancelsOtherSlotsAndPublishesEmailWithIcs() {
        InterviewRound round = sampleRound();
        InterviewSlot slot1 = sampleSlot(round, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.OPEN);
        InterviewSlot slot2 = sampleSlot(round, FIXED_NOW.plus(48, ChronoUnit.HOURS), InterviewSlotStatus.OPEN);

        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(slotRepository.findByIdAndRound_Id(slot1.getId(), ROUND_ID)).thenReturn(Optional.of(slot1));
        when(scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                ROUND_ID, InterviewScheduleStatus.SCHEDULED)).thenReturn(Optional.empty());
        when(rikkeiMeetClient.createRoom(anyString(), eq(slot1.getStartAt()), eq(slot1.getEndAt())))
                .thenReturn("https://meet.rikkei.edu.vn/room-abc123");
        when(slotRepository.findByRound_IdOrderByStartAtAsc(ROUND_ID)).thenReturn(List.of(slot1, slot2));
        when(scheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(inv -> {
            InterviewSchedule s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        var response = service.pickSlot(ROUND_ID, slot1.getId(), candidateActor, "Bearer tok");

        assertEquals("https://meet.rikkei.edu.vn/room-abc123", response.meetUrl());
        assertEquals(InterviewScheduleStatus.SCHEDULED, response.status());
        assertEquals(InterviewSlotStatus.PICKED, slot1.getStatus());
        assertEquals(InterviewSlotStatus.CANCELLED, slot2.getStatus());
        assertNotNull(round.getScheduledAt());

        ArgumentCaptor<InterviewScheduledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewScheduledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        InterviewScheduledEvent published = eventCaptor.getValue();
        assertEquals("candidate1@rikkeijobs.test", published.candidateEmail());
        assertTrue(published.hrRecipients().contains("mentor1@rikkeijobs.test"));
        assertTrue(published.icsContent().contains("BEGIN:VCALENDAR"));
        assertTrue(published.icsContent().contains("https://meet.rikkei.edu.vn/room-abc123"));
    }

    @Test
    void pickSlot_rejectsCancelledSlotWith409() {
        InterviewRound round = sampleRound();
        InterviewSlot cancelledSlot = sampleSlot(round, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.CANCELLED);

        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(slotRepository.findByIdAndRound_Id(cancelledSlot.getId(), ROUND_ID)).thenReturn(Optional.of(cancelledSlot));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.pickSlot(ROUND_ID, cancelledSlot.getId(), candidateActor, "Bearer tok"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SLOT_NOT_OPEN", ex.getCode());
        verify(rikkeiMeetClient, never()).createRoom(anyString(), any(), any());
    }

    @Test
    void pickSlot_rejectsOtherCandidateWith404() {
        InterviewRound round = sampleRound();
        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        UserPrincipal otherCandidate = new UserPrincipal(OTHER_CANDIDATE_ID, "CANDIDATE", null, "candidate2@rikkeijobs.test");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.pickSlot(ROUND_ID, UUID.randomUUID(), otherCandidate, "Bearer tok"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void reschedule_cancelsOldScheduleAndSlotsAndCreatesNewOpenSlots() {
        InterviewRound round = sampleRound();
        InterviewSlot oldSlot = sampleSlot(round, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.PICKED);
        InterviewSchedule oldSchedule = new InterviewSchedule();
        oldSchedule.setId(UUID.randomUUID());
        oldSchedule.setRound(round);
        oldSchedule.setSlot(oldSlot);
        oldSchedule.setMeetUrl("https://meet.rikkei.edu.vn/room-old");
        oldSchedule.setStatus(InterviewScheduleStatus.SCHEDULED);

        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(scheduleRepository.findByRound_IdAndStatus(ROUND_ID, InterviewScheduleStatus.SCHEDULED))
                .thenReturn(List.of(oldSchedule));
        when(slotRepository.findByRound_IdOrderByStartAtAsc(ROUND_ID)).thenReturn(List.of(oldSlot));
        when(slotRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<InterviewSlot> list = inv.getArgument(0);
            for (InterviewSlot s : list) {
                if (s.getId() == null) s.setId(UUID.randomUUID());
            }
            return list;
        });

        var result = service.reschedule(ROUND_ID, List.of(slotInput(72, 73)), hrActor, "Bearer tok");

        assertEquals(1, result.cancelledSchedulesCount());
        assertEquals(1, result.cancelledSlotsCount());
        assertEquals(InterviewScheduleStatus.CANCELLED, oldSchedule.getStatus());
        assertEquals(InterviewSlotStatus.CANCELLED, oldSlot.getStatus());
        assertNull(round.getScheduledAt());
        assertEquals(1, result.slots().size());
        assertEquals(InterviewSlotStatus.OPEN, result.slots().get(0).status());
    }

    @Test
    void sendDueReminders_sendsExactlyOneReminderForOneHourAwayAndDoesNotDuplicateOnRerun() {
        InterviewRound round = sampleRound();
        InterviewSlot slotIn45Min = sampleSlot(round, FIXED_NOW.plus(45, ChronoUnit.MINUTES), InterviewSlotStatus.PICKED);
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setRound(round);
        schedule.setSlot(slotIn45Min);
        schedule.setMeetUrl("https://meet.rikkei.edu.vn/room-1h");
        schedule.setStatus(InterviewScheduleStatus.SCHEDULED);

        // First run: 24h window empty, 1h window returns schedule (reminded1hAt == null)
        when(scheduleRepository.findDueFor24hReminder(any(), any())).thenReturn(List.of());
        when(scheduleRepository.findDueFor1hReminder(any(), any()))
                .thenReturn(List.of(schedule))
                .thenReturn(List.of()); // second run: reminded1hAt is already set

        var firstRun = service.sendDueReminders();
        assertEquals(0, firstRun.sent24hCount());
        assertEquals(1, firstRun.sent1hCount());
        assertEquals(FIXED_NOW, schedule.getReminded1hAt());
        assertNull(schedule.getReminded24hAt());

        var secondRun = service.sendDueReminders();
        assertEquals(0, secondRun.sent24hCount());
        assertEquals(0, secondRun.sent1hCount());

        verify(eventPublisher, times(1)).publishEvent(any(InterviewReminderEvent.class));
    }

    @Test
    void listCompanyInterviews_returnsScheduledItemsInRange() {
        InterviewRound round = sampleRound();
        InterviewSlot slot = sampleSlot(round, FIXED_NOW.plus(10, ChronoUnit.HOURS), InterviewSlotStatus.PICKED);
        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setRound(round);
        schedule.setSlot(slot);
        schedule.setMeetUrl("https://meet.rikkei.edu.vn/room-week");
        schedule.setStatus(InterviewScheduleStatus.SCHEDULED);

        Instant from = FIXED_NOW;
        Instant to = FIXED_NOW.plus(7, ChronoUnit.DAYS);
        when(scheduleRepository.findCompanySchedulesInRange(COMPANY_ID, from, to)).thenReturn(List.of(schedule));

        var items = service.listCompanyInterviews(hrActor, from, to);
        assertEquals(1, items.size());
        assertEquals("https://meet.rikkei.edu.vn/room-week", items.get(0).meetUrl());
        assertEquals("candidate1@rikkeijobs.test", items.get(0).candidateEmail());
    }

    @Test
    void pickSlot_rejectsCandidateScheduleConflictWith409() {
        InterviewRound round = sampleRound();
        InterviewSlot chosenSlot = sampleSlot(round, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.OPEN);

        InterviewRound otherRound = sampleRound();
        otherRound.setId(UUID.randomUUID());
        otherRound.setName("Other Interview Round");
        InterviewSlot otherSlot = sampleSlot(otherRound, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.PICKED);
        InterviewSchedule existingSchedule = new InterviewSchedule();
        existingSchedule.setId(UUID.randomUUID());
        existingSchedule.setRound(otherRound);
        existingSchedule.setSlot(otherSlot);
        existingSchedule.setStatus(InterviewScheduleStatus.SCHEDULED);

        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(slotRepository.findByIdAndRound_Id(chosenSlot.getId(), ROUND_ID)).thenReturn(Optional.of(chosenSlot));
        when(scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                ROUND_ID, InterviewScheduleStatus.SCHEDULED)).thenReturn(Optional.empty());
        when(scheduleRepository.findConflictingCandidateSchedules(
                CANDIDATE_ID, ROUND_ID, chosenSlot.getStartAt(), chosenSlot.getEndAt()))
                .thenReturn(List.of(existingSchedule));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.pickSlot(ROUND_ID, chosenSlot.getId(), candidateActor, "Bearer tok"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("CANDIDATE_SCHEDULE_CONFLICT", ex.getCode());
        verify(rikkeiMeetClient, never()).createRoom(anyString(), any(), any());
    }

    @Test
    void pickSlot_rejectsInterviewerScheduleConflictWith409() {
        InterviewRound round = sampleRound();
        InterviewSlot chosenSlot = sampleSlot(round, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.OPEN);

        InterviewRound otherRound = sampleRound();
        otherRound.setId(UUID.randomUUID());
        otherRound.setName("Another Candidate Interview");
        InterviewSlot otherSlot = sampleSlot(otherRound, FIXED_NOW.plus(24, ChronoUnit.HOURS), InterviewSlotStatus.PICKED);
        InterviewSchedule interviewerSchedule = new InterviewSchedule();
        interviewerSchedule.setId(UUID.randomUUID());
        interviewerSchedule.setRound(otherRound);
        interviewerSchedule.setSlot(otherSlot);
        interviewerSchedule.setStatus(InterviewScheduleStatus.SCHEDULED);

        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(slotRepository.findByIdAndRound_Id(chosenSlot.getId(), ROUND_ID)).thenReturn(Optional.of(chosenSlot));
        when(scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                ROUND_ID, InterviewScheduleStatus.SCHEDULED)).thenReturn(Optional.empty());
        when(scheduleRepository.findConflictingCandidateSchedules(
                CANDIDATE_ID, ROUND_ID, chosenSlot.getStartAt(), chosenSlot.getEndAt()))
                .thenReturn(List.of());
        when(scheduleRepository.findConflictingInterviewerSchedules(
                "mentor1@rikkeijobs.test", ROUND_ID, chosenSlot.getStartAt(), chosenSlot.getEndAt()))
                .thenReturn(List.of(interviewerSchedule));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.pickSlot(ROUND_ID, chosenSlot.getId(), candidateActor, "Bearer tok"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INTERVIEWER_SCHEDULE_CONFLICT", ex.getCode());
        verify(rikkeiMeetClient, never()).createRoom(anyString(), any(), any());
    }

    @Test
    void proposeSlots_rejectsOverlapWithExistingOpenSlotWith409() {
        InterviewRound round = sampleRound();
        InterviewSlot existingOpen = sampleSlot(round, FIXED_NOW.plus(2, ChronoUnit.HOURS), InterviewSlotStatus.OPEN);

        when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
        when(scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                ROUND_ID, InterviewScheduleStatus.SCHEDULED)).thenReturn(Optional.empty());
        when(slotRepository.findByRound_IdAndStatus(ROUND_ID, InterviewSlotStatus.OPEN))
                .thenReturn(List.of(existingOpen));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.proposeSlots(ROUND_ID, List.of(slotInput(2, 3)), hrActor, "Bearer tok"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SLOT_OVERLAP_EXISTING", ex.getCode());
    }

    private InterviewSlotInput slotInput(long plusStartHours, long plusEndHours) {
        return new InterviewSlotInput(
                FIXED_NOW.plus(plusStartHours, ChronoUnit.HOURS),
                FIXED_NOW.plus(plusEndHours, ChronoUnit.HOURS));
    }

    private InterviewRound sampleRound() {
        Recruitment recruitment = new Recruitment();
        recruitment.setId(RECRUITMENT_ID);
        recruitment.setJob_id(JOB_ID);
        recruitment.setUser_id(CANDIDATE_ID);
        recruitment.setCompany_id(COMPANY_ID);
        recruitment.setCandidate_email("candidate1@rikkeijobs.test");
        recruitment.setStatus(RecruitmentStatus.INTERVIEW);

        InterviewRound round = new InterviewRound();
        round.setId(ROUND_ID);
        round.setRecruitment(recruitment);
        round.setRoundNo(1);
        round.setName("Technical Interview");
        round.setInterviewerEmail("mentor1@rikkeijobs.test");
        round.setResult(InterviewRoundResult.PENDING);
        return round;
    }

    private InterviewSlot sampleSlot(InterviewRound round, Instant startAt, InterviewSlotStatus status) {
        InterviewSlot slot = new InterviewSlot();
        slot.setId(UUID.randomUUID());
        slot.setRound(round);
        slot.setStartAt(startAt);
        slot.setEndAt(startAt.plus(1, ChronoUnit.HOURS));
        slot.setStatus(status);
        return slot;
    }
}
