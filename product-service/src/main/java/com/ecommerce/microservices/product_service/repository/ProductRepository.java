package com.ecommerce.microservices.product_service.repository;

import com.ecommerce.microservices.product_service.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);
}
