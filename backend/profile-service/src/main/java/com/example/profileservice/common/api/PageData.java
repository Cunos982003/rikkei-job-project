package com.example.profileservice.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageData<T>(
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

    public static <T> PageData<T> from(Page<T> springPage) {
        return new PageData<>(
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
}