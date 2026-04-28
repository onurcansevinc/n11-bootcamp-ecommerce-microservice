package com.ecommerce.microservices.product_service.service;

import com.ecommerce.microservices.product_service.dto.ProductResponse;
import com.ecommerce.microservices.product_service.entity.Product;
import com.ecommerce.microservices.product_service.exception.InvalidProductFilterException;
import com.ecommerce.microservices.product_service.exception.ProductNotFoundException;
import com.ecommerce.microservices.product_service.repository.ProductRepository;
import com.ecommerce.microservices.product_service.repository.specification.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(
            int page,
            int size,
            String search,
            Boolean active,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        validatePriceRange(minPrice, maxPrice);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Specification<Product> specification = ProductSpecifications.withSearch(search)
                .and(ProductSpecifications.hasActive(active))
                .and(ProductSpecifications.hasCategoryId(categoryId))
                .and(ProductSpecifications.priceGreaterThanOrEqual(minPrice))
                .and(ProductSpecifications.priceLessThanOrEqual(maxPrice));

        return productRepository.findAll(
                        specification,
                        pageRequest
                )
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        return productRepository.findById(productId)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidProductFilterException("minPrice cannot be greater than maxPrice");
        }
    }
}
