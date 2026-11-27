package com.example.profileservice.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfSkillDto {
    private UUID skillId;
    private Integer selfScore;
}