package com.example.recruitment_service.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.enums.InterviewScheduleStatus;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, UUID> {
    Optional<InterviewSchedule> findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
            UUID roundId,
            InterviewScheduleStatus status);

    List<InterviewSchedule> findByRound_IdAndStatus(UUID roundId, InterviewScheduleStatus status);

    @Query("""
            SELECT s FROM InterviewSchedule s
            JOIN FETCH s.slot sl
            JOIN FETCH s.round r
            JOIN FETCH r.recruitment rec
            WHERE r.id IN :roundIds
            ORDER BY s.createdAt DESC
            """)
    List<InterviewSchedule> findAllWithDetailsByRoundIds(@Param("roundIds") Collection<UUID> roundIds);

    @Query("""
            SELECT s FROM InterviewSchedule s
            JOIN FETCH s.slot sl
            JOIN FETCH s.round r
            JOIN FETCH r.recruitment rec
            WHERE rec.company_id = :companyId
              AND sl.startAt >= :from
              AND sl.startAt <= :to
            ORDER BY sl.startAt ASC
            """)
    List<InterviewSchedule> findCompanySchedulesInRange(
            @Param("companyId") UUID companyId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT s FROM InterviewSchedule s
            JOIN FETCH s.slot sl
            JOIN FETCH s.round r
            JOIN FETCH r.recruitment rec
            WHERE s.status = com.example.recruitment_service.enums.InterviewScheduleStatus.SCHEDULED
              AND s.reminded24hAt IS NULL
              AND sl.startAt > :windowStart
              AND sl.startAt <= :windowEnd
            ORDER BY sl.startAt ASC
            """)
    List<InterviewSchedule> findDueFor24hReminder(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);

    @Query("""
            SELECT s FROM InterviewSchedule s
            JOIN FETCH s.slot sl
            JOIN FETCH s.round r
            JOIN FETCH r.recruitment rec
            WHERE s.status = com.example.recruitment_service.enums.InterviewScheduleStatus.SCHEDULED
              AND s.reminded1hAt IS NULL
              AND sl.startAt > :windowStart
              AND sl.startAt <= :windowEnd
            ORDER BY sl.startAt ASC
            """)
    List<InterviewSchedule> findDueFor1hReminder(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);
}
