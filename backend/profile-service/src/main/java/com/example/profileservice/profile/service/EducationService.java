package com.example.profileservice.profile.service;

import com.example.profileservice.profile.api.dto.EducationDto;
import com.example.profileservice.profile.domain.Education;
import com.example.profileservice.profile.infrastructure.EducationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducationService {

    private final EducationRepository educationRepo;

    @Transactional
    public EducationDto createEducation(UUID userId, EducationDto dto) {
        Education education = Education.builder()
                .userId(userId)
                .school(dto.getSchool())
                .major(dto.getMajor())
                .fromDate(dto.getFromDate())
                .toDate(dto.getToDate())
                .gpa(dto.getGpa())
                .note(dto.getNote())
                .build();

        Education saved = educationRepo.save(education);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EducationDto> getEducations(UUID userId) {
        return educationRepo.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EducationDto getEducation(UUID userId, UUID educationId) {
        Education education = educationRepo.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Education not found"));
        if (!education.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Education not found");
        }
        return toDto(education);
    }

    @Transactional
    public EducationDto updateEducation(UUID userId, UUID educationId, EducationDto dto) {
        Education education = educationRepo.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Education not found"));
        if (!education.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Education not found");
        }

        education.setSchool(dto.getSchool());
        education.setMajor(dto.getMajor());
        education.setFromDate(dto.getFromDate());
        education.setToDate(dto.getToDate());
        education.setGpa(dto.getGpa());
        education.setNote(dto.getNote());

        Education saved = educationRepo.save(education);
        return toDto(saved);
    }

    @Transactional
    public void deleteEducation(UUID userId, UUID educationId) {
        Education education = educationRepo.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Education not found"));
        if (!education.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Education not found");
        }
        educationRepo.delete(education);
    }

    private EducationDto toDto(Education e) {
        return EducationDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .school(e.getSchool())
                .major(e.getMajor())
                .fromDate(e.getFromDate())
                .toDate(e.getToDate())
                .gpa(e.getGpa())
                .note(e.getNote())
                .build();
    }
}