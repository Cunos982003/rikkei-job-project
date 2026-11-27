package com.example.learningservice.model.dto.lms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LmsCourseDto {
    private String courseId;
    private String name;
    private List<String> skills;
    private String level;
}

