package com.example.jobservice.company.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        PaginationMeta pagination,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public List<T> getContent() {
        return items;
    }
}

