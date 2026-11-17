package com.example.learningservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshEnrollmentsResponse {
    private int totalUpdated;
    private int completedCoursesCount;
    private List<EnrollmentResponse> enrollments;
}

