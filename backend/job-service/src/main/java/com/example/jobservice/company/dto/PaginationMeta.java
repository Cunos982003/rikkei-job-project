package com.example.jobservice.company.dto;

public record PaginationMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious
) {}

