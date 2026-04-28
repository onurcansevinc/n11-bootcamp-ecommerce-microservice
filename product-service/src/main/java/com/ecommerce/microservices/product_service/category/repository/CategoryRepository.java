package com.ecommerce.microservices.product_service.category.repository;

import com.ecommerce.microservices.product_service.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
