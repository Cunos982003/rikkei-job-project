package com.example.profileservice.profile.service; // Chú ý package của bạn

import com.example.profileservice.internship.api.dto.InternshipResponse;
import com.example.profileservice.internship.api.dto.InternshipTaskResponse;
import com.example.profileservice.internship.api.dto.PmInternshipMockDto;
import com.example.profileservice.internship.domain.Internship;
import com.example.profileservice.internship.domain.InternshipTask;
import com.example.profileservice.internship.infrastructure.InternshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InternshipService {

    private final InternshipRepository repository;

    public InternshipService(InternshipRepository repository) {
        this.repository = repository;
    }

    public List<InternshipResponse> getInternshipsByUserId(UUID userId) { // Sửa Long thành UUID
        return repository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void syncFromPmMock(UUID userId, List<PmInternshipMockDto> mockDataList) { // Sửa Long thành UUID
        for (PmInternshipMockDto dto : mockDataList) {
            // Sửa hàm tìm kiếm nhận String batchId
            Internship internship = repository.findByUserIdAndBatchId(userId, dto.batchId())
                    .orElseGet(Internship::new);

            internship.setUserId(userId); // userId giờ là UUID
            internship.setBatchId(dto.batchId()); // batchId giờ là String
            internship.setBatchName(dto.batchName());
            internship.setGroupName(dto.groupName());
            internship.setTopicName(dto.topicName());
            internship.setMentorEmail(dto.mentorEmail());
            internship.setStartDate(dto.startDate());
            internship.setEndDate(dto.endDate());
            internship.setFinalScore(dto.finalScore());
            internship.setSyncedAt(OffsetDateTime.now());

            internship.getTasks().clear();
            if (dto.tasks() != null) {
                for (PmInternshipMockDto.PmTaskMockDto t : dto.tasks()) {
                    InternshipTask task = new InternshipTask();
                    task.setId(UUID.randomUUID()); // Cấp UUID cho Task mới
                    task.setTitle(t.title());
                    task.setAcceptedScore(t.acceptedScore());
                    internship.getTasks().add(task);
                    task.setInternship(internship);
                }
            }

            repository.save(internship);
        }
    }

    private InternshipResponse mapToResponse(Internship entity) {
        // Map TaskResponse (t.getId() giờ là UUID)
        List<InternshipTaskResponse> taskDtos = entity.getTasks().stream()
                .map(t -> new InternshipTaskResponse(
                        t.getId(),
                        t.getTitle(),
                        t.getAcceptedScore(),
                        t.getAcceptedAt()
                ))
                .collect(Collectors.toList());

        // Map InternshipResponse (entity.getId() giờ là UUID)
        return new InternshipResponse(
                entity.getId(),
                entity.getBatchName(),
                entity.getGroupName(),
                entity.getTopicName(),
                entity.getMentorEmail(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getFinalScore(),
                entity.getSyncedAt(),
                taskDtos
        );
    }
}