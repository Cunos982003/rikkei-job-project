package com.example.recruitment_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.entity.InterviewSlot;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.InterviewScheduleStatus;
import com.example.recruitment_service.enums.InterviewSlotStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.InterviewScheduleRepository;
import com.example.recruitment_service.repository.InterviewSlotRepository;
import com.example.recruitment_service.repository.RecruitmentRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InterviewReminderIntegrationTest {
    @Autowired
    private RecruitmentRepository recruitmentRepository;
    @Autowired
    private InterviewRoundRepository roundRepository;
    @Autowired
    private InterviewSlotRepository slotRepository;
    @Autowired
    private InterviewScheduleRepository scheduleRepository;
    @Autowired
    private InterviewScheduleService scheduleService;

    @MockitoBean
    private RecruitmentEmailNotifier emailNotifier;

    @Test
    void reminderJobPersistsReminded1hAtAndDoesNotResendOnSecondRun() {
        Instant now = Instant.now(Clock.systemUTC());

        Recruitment rec = new Recruitment();
        rec.setJob_id(UUID.randomUUID());
        rec.setUser_id(UUID.randomUUID());
        rec.setCv_id(UUID.randomUUID());
        rec.setCompany_id(UUID.randomUUID());
        rec.setCandidate_email("candidate-reminder@rikkeijobs.test");
        rec.setCv_snapshot_url("/cv/test.pdf");
        rec.setStatus(RecruitmentStatus.INTERVIEW);
        rec = recruitmentRepository.save(rec);

        InterviewRound round = new InterviewRound();
        round.setRecruitment(rec);
        round.setRoundNo(1);
        round.setName("Technical Round");
        round.setInterviewerEmail("hr-reminder@rikkeijobs.test");
        round.setResult(InterviewRoundResult.PENDING);
        round = roundRepository.save(round);

        InterviewSlot slot = new InterviewSlot();
        slot.setRound(round);
        slot.setStartAt(now.plus(45, ChronoUnit.MINUTES));
        slot.setEndAt(now.plus(105, ChronoUnit.MINUTES));
        slot.setStatus(InterviewSlotStatus.PICKED);
        slot = slotRepository.save(slot);

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setRound(round);
        schedule.setSlot(slot);
        schedule.setMeetUrl("https://meet.rikkei.edu.vn/room-int-1h");
        schedule.setStatus(InterviewScheduleStatus.SCHEDULED);
        schedule = scheduleRepository.save(schedule);

        var run1 = scheduleService.sendDueReminders();
        assertEquals(0, run1.sent24hCount());
        assertEquals(1, run1.sent1hCount());

        InterviewSchedule reloaded = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertNotNull(reloaded.getReminded1hAt());

        var run2 = scheduleService.sendDueReminders();
        assertEquals(0, run2.sent24hCount());
        assertEquals(0, run2.sent1hCount());
    }

    @Test
    void conflictQueriesDetectOverlappingRangesAndAllowBackToBackSlots() {
        Instant base = Instant.now(Clock.systemUTC()).plus(24, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        UUID candidateId = UUID.randomUUID();

        Recruitment rec = new Recruitment();
        rec.setJob_id(UUID.randomUUID());
        rec.setUser_id(candidateId);
        rec.setCv_id(UUID.randomUUID());
        rec.setCompany_id(UUID.randomUUID());
        rec.setCandidate_email("overlap@rikkeijobs.test");
        rec.setCv_snapshot_url("/cv/overlap.pdf");
        rec.setStatus(RecruitmentStatus.INTERVIEW);
        rec = recruitmentRepository.save(rec);

        InterviewRound round1 = new InterviewRound();
        round1.setRecruitment(rec);
        round1.setRoundNo(1);
        round1.setName("Round 1");
        round1.setInterviewerEmail("interviewer-conflict@rikkeijobs.test");
        round1.setResult(InterviewRoundResult.PENDING);
        round1 = roundRepository.save(round1);

        InterviewSlot slot1 = new InterviewSlot();
        slot1.setRound(round1);
        slot1.setStartAt(base);
        slot1.setEndAt(base.plus(1, ChronoUnit.HOURS));
        slot1.setStatus(InterviewSlotStatus.PICKED);
        slot1 = slotRepository.save(slot1);

        InterviewSchedule schedule1 = new InterviewSchedule();
        schedule1.setRound(round1);
        schedule1.setSlot(slot1);
        schedule1.setMeetUrl("https://meet.rikkei.edu.vn/room-conflict-1");
        schedule1.setStatus(InterviewScheduleStatus.SCHEDULED);
        scheduleRepository.saveAndFlush(schedule1);

        UUID otherRoundId = UUID.randomUUID();
        // Overlapping 30m into slot1 -> MUST detect conflict for both candidate and interviewer
        assertEquals(1, scheduleRepository.findConflictingCandidateSchedules(
                candidateId, otherRoundId, base.plus(30, ChronoUnit.MINUTES), base.plus(90, ChronoUnit.MINUTES)).size());
        assertEquals(1, scheduleRepository.findConflictingInterviewerSchedules(
                "INTERVIEWER-CONFLICT@rikkeijobs.test", otherRoundId, base.plus(30, ChronoUnit.MINUTES), base.plus(90, ChronoUnit.MINUTES)).size());

        // Back-to-back (starts exactly when slot1 ends) -> NO conflict
        assertEquals(0, scheduleRepository.findConflictingCandidateSchedules(
                candidateId, otherRoundId, base.plus(1, ChronoUnit.HOURS), base.plus(2, ChronoUnit.HOURS)).size());
        assertEquals(0, scheduleRepository.findConflictingInterviewerSchedules(
                "interviewer-conflict@rikkeijobs.test", otherRoundId, base.plus(1, ChronoUnit.HOURS), base.plus(2, ChronoUnit.HOURS)).size());
    }
}
