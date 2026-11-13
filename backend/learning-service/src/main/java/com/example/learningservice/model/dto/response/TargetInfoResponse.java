package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetInfoResponse {
    private TargetType type;
    private String id;
    private String code;
    private String name;
    private String description;
}

