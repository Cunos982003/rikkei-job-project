package com.example.profileservice.profile.api.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffEditSelfDeclaredResponse {
    private UUID userId;
    private StaffEditSelfDeclaredRequest.Field field;
    private String oldValue;
    private String newValue;
}