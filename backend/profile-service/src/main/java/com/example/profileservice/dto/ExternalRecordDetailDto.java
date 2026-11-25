package com.example.profileservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalRecordDetailDto {
    private UUID id;
    private UUID userId;
    private String source;
    private String sourceKey;
    private Object payload;
    private OffsetDateTime syncedAt;

    @JsonProperty("isOverridden")
    private boolean isOverridden;

    private List<ManualOverrideDto> overrides;

    @JsonProperty("overridden")
    public boolean isOverriddenAlias() {
        return isOverridden;
    }
}
