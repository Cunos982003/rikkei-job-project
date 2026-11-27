package com.example.learningservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {

    @NotBlank(message = "Mã khóa học LMS (lmsCourseId) không được để trống")
    private String lmsCourseId;

    @NotBlank(message = "Mã kỹ năng (skillId) không được để trống")
    private String skillId;
}

