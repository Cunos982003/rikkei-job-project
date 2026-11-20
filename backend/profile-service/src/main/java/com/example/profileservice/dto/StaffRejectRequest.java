package com.example.profileservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRejectRequest {
    private String reason;
}
