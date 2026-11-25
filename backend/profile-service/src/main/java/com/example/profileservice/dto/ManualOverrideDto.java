package com.example.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualOverrideDto {
    private UUID id;
    private UUID recordId;
    private String field;
    private String value;
    private String reason;
    private UUID staffId;
    private OffsetDateTime createdAt;
}
