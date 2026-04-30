package com.ecommerce.microservices.common.web.response;

import org.springframework.data.domain.Page;

public record ResponseMeta(
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        Boolean hasNext,
        Boolean hasPrevious
) {
    public static ResponseMeta from(Page<?> page) {
        return new ResponseMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
