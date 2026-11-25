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
public class SyncRunResponseDto {
    private UUID id;
    private String trigger;
    private UUID userId;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String error;
    private Object counts;
}
