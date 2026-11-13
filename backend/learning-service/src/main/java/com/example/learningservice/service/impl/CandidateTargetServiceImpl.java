package com.example.learningservice.service.impl;

import com.example.learningservice.client.JobServiceClient;
import com.example.learningservice.exception.ResourceNotFoundException;
import com.example.learningservice.model.dto.request.TargetSelectionRequest;
import com.example.learningservice.model.dto.response.CandidateTargetResponse;
import com.example.learningservice.model.dto.response.JobDetailResponse;
import com.example.learningservice.model.entity.CandidateTarget;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.CandidateTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateTargetServiceImpl implements CandidateTargetService {

    private final CandidateTargetRepository candidateTargetRepository;
    private final TargetPositionRepository targetPositionRepository;
    private final JobServiceClient jobServiceClient;

    @Override
    @Transactional
    public CandidateTargetResponse setTarget(UUID userId, TargetSelectionRequest request) {
        log.info("Thiết lập mục tiêu cho userId: {}, positionId: {}, jobId: {}",
                userId, request.getPositionId(), request.getJobId());

        TargetType targetType;
        String targetName = "";
        UUID positionId = null;
        String jobId = null;

        if (request.getPositionId() != null) {
            targetType = TargetType.POSITION;
            UUID selectedPositionId = request.getPositionId();
            positionId = selectedPositionId;
            TargetPosition position = targetPositionRepository.findById(selectedPositionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí mục tiêu với ID: " + selectedPositionId));
            targetName = position.getName();
        } else if (request.getJobId() != null && !request.getJobId().isBlank()) {
            targetType = TargetType.JOB;
            String selectedJobId = request.getJobId().trim();
            jobId = selectedJobId;
            JobDetailResponse job = jobServiceClient.getJobById(selectedJobId);
            targetName = job != null ? job.getTitle() : selectedJobId;
        } else {
            throw new IllegalArgumentException("Phải cung cấp positionId hoặc jobId.");
        }

        CandidateTarget target = candidateTargetRepository.findByUserId(userId)
                .orElseGet(() -> CandidateTarget.builder().userId(userId).build());

        target.setTargetType(targetType);
        target.setPositionId(positionId);
        target.setJobId(jobId);
        target.setChosenAt(Instant.now());

        CandidateTarget saved = candidateTargetRepository.save(target);

        return CandidateTargetResponse.builder()
                .userId(saved.getUserId())
                .targetType(saved.getTargetType())
                .positionId(saved.getPositionId())
                .jobId(saved.getJobId())
                .targetName(targetName)
                .chosenAt(saved.getChosenAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CandidateTargetResponse> getCurrentTarget(UUID userId) {
        return candidateTargetRepository.findByUserId(userId).map(target -> {
            String targetName = "";
            if (target.getTargetType() == TargetType.POSITION && target.getPositionId() != null) {
                targetName = targetPositionRepository.findById(target.getPositionId())
                        .map(TargetPosition::getName)
                        .orElse("Vị trí không xác định");
            } else if (target.getTargetType() == TargetType.JOB && target.getJobId() != null) {
                try {
                    JobDetailResponse job = jobServiceClient.getJobById(target.getJobId());
                    targetName = job != null ? job.getTitle() : target.getJobId();
                } catch (Exception e) {
                    targetName = target.getJobId();
                }
            }

            return CandidateTargetResponse.builder()
                    .userId(target.getUserId())
                    .targetType(target.getTargetType())
                    .positionId(target.getPositionId())
                    .jobId(target.getJobId())
                    .targetName(targetName)
                    .chosenAt(target.getChosenAt())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CandidateTarget> getTargetEntity(UUID userId) {
        return candidateTargetRepository.findByUserId(userId);
    }
}

