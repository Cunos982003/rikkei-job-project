package com.example.profileservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishRequest {
    private Boolean consent;
}
