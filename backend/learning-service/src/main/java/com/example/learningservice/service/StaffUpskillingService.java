package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import com.example.learningservice.model.dto.response.staff.UpskillingSummaryResponse;

import java.util.List;

public interface StaffUpskillingService {

    List<UpskillingCandidateDto> getUpskillingList(String courseName, String classCode, String status, String skillId, Integer page, Integer size);

    long countUpskillingList(String courseName, String classCode, String status, String skillId);

    UpskillingSummaryResponse getUpskillingSummary();
}
