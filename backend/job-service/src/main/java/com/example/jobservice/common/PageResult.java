package com.example.jobservice.common;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> items,
        PaginationMeta pagination
) {
    public record PaginationMeta(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean isFirst,
            boolean isLast,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public PaginationMeta(int page, int size, long totalElements, int totalPages, boolean isFirst, boolean isLast) {
            this(page, size, totalElements, totalPages, isFirst, isLast, page < totalPages - 1, page > 0);
        }
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, long totalElements) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        boolean isFirst = safePage == 0;
        boolean isLast = totalPages == 0 || safePage >= totalPages - 1;
        boolean hasNext = safePage < totalPages - 1;
        boolean hasPrevious = safePage > 0;
        return new PageResult<>(
                items != null ? items : List.of(),
                new PaginationMeta(safePage, safeSize, totalElements, totalPages, isFirst, isLast, hasNext, hasPrevious)
        );
    }

    public static <T> PageResult<T> from(Page<T> springPage) {
        return new PageResult<>(
                springPage.getContent(),
                new PaginationMeta(
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

    public static <T, R> PageResult<R> from(Page<T> springPage, Function<T, R> mapper) {
        List<R> mappedItems = springPage.getContent().stream().map(mapper).toList();
        return new PageResult<>(
                mappedItems,
                new PaginationMeta(
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

