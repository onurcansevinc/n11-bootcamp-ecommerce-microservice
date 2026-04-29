package com.ecommerce.microservices.product_service.product.service;

import com.ecommerce.microservices.product_service.category.entity.Category;
import com.ecommerce.microservices.product_service.category.exception.CategoryNotFoundException;
import com.ecommerce.microservices.product_service.category.repository.CategoryRepository;
import com.ecommerce.microservices.product_service.common.cache.CacheNames;
import com.ecommerce.microservices.product_service.product.dto.ProductPatchRequest;
import com.ecommerce.microservices.product_service.product.dto.ProductResponse;
import com.ecommerce.microservices.product_service.product.dto.ProductUpsertRequest;
import com.ecommerce.microservices.product_service.product.entity.Product;
import com.ecommerce.microservices.product_service.product.exception.DuplicateProductSkuException;
import com.ecommerce.microservices.product_service.product.exception.InvalidProductFilterException;
import com.ecommerce.microservices.product_service.product.exception.InvalidProductPatchException;
import com.ecommerce.microservices.product_service.product.exception.ProductNotFoundException;
import com.ecommerce.microservices.product_service.product.repository.ProductRepository;
import com.ecommerce.microservices.product_service.product.repository.specification.ProductSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
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

    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#result.id")
    public ProductResponse createProduct(ProductUpsertRequest request) {
        validateUniqueSku(request.sku(), null);
        Category category = getCategoryById(request.categoryId());

        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.sku(),
                request.active(),
                category
        );

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#productId")
    public ProductResponse updateProduct(Long productId, ProductUpsertRequest request) {
        Product product = getProductByIdInternal(productId);
        validateUniqueSku(request.sku(), productId);
        Category category = getCategoryById(request.categoryId());

        product.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.sku(),
                request.active(),
                category
        );

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#productId")
    public ProductResponse patchProduct(Long productId, ProductPatchRequest request) {
        if (!request.hasChanges()) {
            throw new InvalidProductPatchException("At least one field must be provided for patch");
        }

        Product product = getProductByIdInternal(productId);

        String finalSku = request.sku() != null ? request.sku() : product.getSku();
        validateUniqueSku(finalSku, productId);

        Category category = request.categoryId() != null
                ? getCategoryById(request.categoryId())
                : product.getCategory();

        product.updateDetails(
                request.name() != null ? request.name() : product.getName(),
                request.description() != null ? request.description() : product.getDescription(),
                request.price() != null ? request.price() : product.getPrice(),
                finalSku,
                request.active() != null ? request.active() : product.getActive(),
                category
        );

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#productId")
    public void deleteProduct(Long productId) {
        Product product = getProductByIdInternal(productId);
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#productId")
    public ProductResponse getProductById(Long productId) {
        return ProductResponse.from(getProductByIdInternal(productId));
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidProductFilterException("minPrice cannot be greater than maxPrice");
        }
    }

    private void validateUniqueSku(String sku, Long currentProductId) {
        boolean skuExists = currentProductId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, currentProductId);

        if (skuExists) {
            throw new DuplicateProductSkuException(sku);
        }
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private Product getProductByIdInternal(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
