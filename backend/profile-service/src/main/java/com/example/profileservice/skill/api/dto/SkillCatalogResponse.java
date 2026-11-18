package com.example.profileservice.skill.api.dto;

/**
 * Phản hồi thông tin kỹ năng trong danh mục chuẩn hệ thống.
 */
public record SkillCatalogResponse(
        String id,
        String code,
        String name,
        String group
) {}

