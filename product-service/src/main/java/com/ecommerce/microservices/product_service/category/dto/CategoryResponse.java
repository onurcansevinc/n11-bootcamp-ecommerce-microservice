package com.ecommerce.microservices.product_service.category.dto;

import com.ecommerce.microservices.product_service.category.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        boolean active
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.isActive()
        );
    }
}
