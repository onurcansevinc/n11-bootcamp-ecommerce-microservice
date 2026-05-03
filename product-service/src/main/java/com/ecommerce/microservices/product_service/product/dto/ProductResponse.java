package com.ecommerce.microservices.product_service.product.dto;

import com.ecommerce.microservices.product_service.category.entity.Category;
import com.ecommerce.microservices.product_service.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Boolean active,
        String campaignLabel,
        String mainImageUrl,
        CategorySummary category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product, String mainImageUrl) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.getActive(),
                product.getCampaignLabel(),
                mainImageUrl,
                CategorySummary.from(product.getCategory()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public record CategorySummary(
            Long id,
            String name,
            String slug
    ) {
        public static CategorySummary from(Category category) {
            if (category == null) {
                return null;
            }

            return new CategorySummary(
                    category.getId(),
                    category.getName(),
                    category.getSlug()
            );
        }
    }
}
