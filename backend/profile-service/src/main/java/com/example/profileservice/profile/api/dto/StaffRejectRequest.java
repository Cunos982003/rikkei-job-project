package com.example.profileservice.profile.api.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRejectRequest {
    private String reason;
}
