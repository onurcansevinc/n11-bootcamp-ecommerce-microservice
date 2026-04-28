package com.ecommerce.microservices.product_service.category.service;

import com.ecommerce.microservices.product_service.category.dto.CategoryPatchRequest;
import com.ecommerce.microservices.product_service.category.dto.CategoryResponse;
import com.ecommerce.microservices.product_service.category.dto.CategoryUpsertRequest;
import com.ecommerce.microservices.product_service.category.entity.Category;
import com.ecommerce.microservices.product_service.category.exception.CategoryInUseException;
import com.ecommerce.microservices.product_service.category.exception.CategoryNotFoundException;
import com.ecommerce.microservices.product_service.category.exception.DuplicateCategorySlugException;
import com.ecommerce.microservices.product_service.category.exception.InvalidCategoryPatchException;
import com.ecommerce.microservices.product_service.category.repository.CategoryRepository;
import com.ecommerce.microservices.product_service.category.repository.specification.CategorySpecifications;
import com.ecommerce.microservices.product_service.common.cache.CacheNames;
import com.ecommerce.microservices.product_service.product.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(int page, int size, String search, Boolean active) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Specification<Category> specification = CategorySpecifications.withSearch(search)
                .and(CategorySpecifications.hasActive(active));

        return categoryRepository.findAll(specification, pageRequest)
                .map(CategoryResponse::from);
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.CATEGORY_BY_ID, key = "#result.id")
    public CategoryResponse createCategory(CategoryUpsertRequest request) {
        validateUniqueSlug(request.slug(), null);

        Category category = new Category(
                request.name(),
                request.slug(),
                request.active()
        );

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheNames.CATEGORY_BY_ID, key = "#categoryId"),
            evict = @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, allEntries = true)
    )
    public CategoryResponse updateCategory(Long categoryId, CategoryUpsertRequest request) {
        Category category = getCategoryByIdInternal(categoryId);
        validateUniqueSlug(request.slug(), categoryId);

        category.updateDetails(request.name(), request.slug(), request.active());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheNames.CATEGORY_BY_ID, key = "#categoryId"),
            evict = @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, allEntries = true)
    )
    public CategoryResponse patchCategory(Long categoryId, CategoryPatchRequest request) {
        if (!request.hasChanges()) {
            throw new InvalidCategoryPatchException("At least one field must be provided for patch");
        }

        Category category = getCategoryByIdInternal(categoryId);
        String finalSlug = request.slug() != null ? request.slug() : category.getSlug();
        validateUniqueSlug(finalSlug, categoryId);

        category.updateDetails(
                request.name() != null ? request.name() : category.getName(),
                finalSlug,
                request.active() != null ? request.active() : category.isActive()
        );

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORY_BY_ID, key = "#categoryId")
    public void deleteCategory(Long categoryId) {
        Category category = getCategoryByIdInternal(categoryId);

        if (productRepository.existsByCategoryId(categoryId)) {
            throw new CategoryInUseException(categoryId);
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CATEGORY_BY_ID, key = "#categoryId")
    public CategoryResponse getCategoryById(Long categoryId) {
        return CategoryResponse.from(getCategoryByIdInternal(categoryId));
    }

    private void validateUniqueSlug(String slug, Long currentCategoryId) {
        boolean slugExists = currentCategoryId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, currentCategoryId);

        if (slugExists) {
            throw new DuplicateCategorySlugException(slug);
        }
    }

    private Category getCategoryByIdInternal(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
