package com.example.profileservice.profile.api.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LmsStudentDto {
    private String fullName;
    private String email;
    private String program;
    private String classCode;
    private String courseName;
}