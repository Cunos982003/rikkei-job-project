package com.example.recruitment_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.recruitment_service.entity.InterviewSlot;
import com.example.recruitment_service.enums.InterviewSlotStatus;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, UUID> {
    List<InterviewSlot> findByRound_IdOrderByStartAtAsc(UUID roundId);

    List<InterviewSlot> findByRound_IdInOrderByStartAtAsc(Collection<UUID> roundIds);

    Optional<InterviewSlot> findByIdAndRound_Id(UUID id, UUID roundId);

    List<InterviewSlot> findByRound_IdAndStatus(UUID roundId, InterviewSlotStatus status);
}
