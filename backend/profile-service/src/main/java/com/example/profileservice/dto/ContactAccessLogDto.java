package com.example.profileservice.dto;

import com.example.profileservice.entity.ContactAccessLog;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactAccessLogDto {
    private UUID id;
    private UUID companyId;
    private UUID hrId;
    private UUID userId;
    private ContactAccessLog.ViaType via;
    private OffsetDateTime createdAt;

    public static ContactAccessLogDto from(ContactAccessLog e) {
        return ContactAccessLogDto.builder()
                .id(e.getId())
                .companyId(e.getCompanyId())
                .hrId(e.getHrId())
                .userId(e.getUserId())
                .via(e.getVia())
                .createdAt(e.getCreatedAt())
                .build();
    }
}