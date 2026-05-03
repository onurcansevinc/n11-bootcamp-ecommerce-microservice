package com.ecommerce.microservices.product_service.product.repository;

import com.ecommerce.microservices.product_service.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdInOrderByProductIdAscMainImageDescIdAsc(Collection<Long> productIds);

    Optional<ProductImage> findFirstByProductIdOrderByMainImageDescIdAsc(Long productId);
}
