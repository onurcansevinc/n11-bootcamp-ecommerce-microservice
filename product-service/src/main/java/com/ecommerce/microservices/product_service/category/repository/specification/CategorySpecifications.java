package com.ecommerce.microservices.product_service.category.repository.specification;

import com.ecommerce.microservices.product_service.category.entity.Category;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class CategorySpecifications {

    private CategorySpecifications() {
    }

    public static Specification<Category> withSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedSearch = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), normalizedSearch),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), normalizedSearch)
            );
        };
    }

    public static Specification<Category> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("active"), active);
        };
    }
}
