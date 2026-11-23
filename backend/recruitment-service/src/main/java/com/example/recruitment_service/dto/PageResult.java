package com.example.recruitment_service.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResult<T>(
        List<T> items,
        PaginationInfo pagination
) {
    public record PaginationInfo(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean isFirst,
            boolean isLast,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    public static <T> PageResult<T> from(Page<T> springPage) {
        return new PageResult<>(
                springPage.getContent(),
                new PaginationInfo(
                        springPage.getNumber(),
                        springPage.getSize(),
                        springPage.getTotalElements(),
                        springPage.getTotalPages(),
                        springPage.isFirst(),
                        springPage.isLast(),
                        springPage.hasNext(),
                        springPage.hasPrevious()
                )
        );
    }

    // Tương thích ngược với các trường Spring Page (content, number, first, last) để không gây vỡ code client cũ nếu có
    public List<T> getContent() {
        return items;
    }

    public int getNumber() {
        return pagination != null ? pagination.page() : 0;
    }

    public int getSize() {
        return pagination != null ? pagination.size() : 0;
    }

    public long getTotalElements() {
        return pagination != null ? pagination.totalElements() : 0;
    }

    public int getTotalPages() {
        return pagination != null ? pagination.totalPages() : 0;
    }

    public boolean isFirst() {
        return pagination != null && pagination.isFirst();
    }

    public boolean isLast() {
        return pagination != null && pagination.isLast();
    }
}
