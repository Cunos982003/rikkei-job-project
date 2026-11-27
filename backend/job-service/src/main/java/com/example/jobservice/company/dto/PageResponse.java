package com.example.jobservice.company.dto;

import com.example.jobservice.common.PageResult;
import java.util.List;

public record PageResponse<T>(
        List<T> items,
        PageResult.PaginationMeta pagination) {
       public static <T> PageResponse<T> from(PageResult<T> pageResult) {
        return new PageResponse<>(pageResult.items(), pageResult.pagination());
    }

    public List<T> getContent() {
        return items;
    }
}
