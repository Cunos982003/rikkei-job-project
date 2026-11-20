package com.example.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for skill catalog response.
 * Used in public skill catalog API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCatalogResponse {

    private String id;
    private String code;
    private String name;
    private String group;
}