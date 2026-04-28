package com.ecommerce.microservices.product_service.category.controller;

import com.ecommerce.microservices.product_service.category.dto.CategoryPatchRequest;
import com.ecommerce.microservices.product_service.category.dto.CategoryResponse;
import com.ecommerce.microservices.product_service.category.dto.CategoryUpsertRequest;
import com.ecommerce.microservices.product_service.category.service.CategoryService;
import com.ecommerce.microservices.product_service.common.response.ApiResponse;
import com.ecommerce.microservices.product_service.common.response.ResponseMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        Page<CategoryResponse> categoryPage = categoryService.getAllCategories(page, size, search, active);

        return ApiResponse.success(
                "Categories fetched successfully",
                categoryPage.getContent(),
                ResponseMeta.from(categoryPage)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", categoryService.createCategory(request)));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpsertRequest request
    ) {
        return ApiResponse.success("Category updated successfully", categoryService.updateCategory(categoryId, request));
    }

    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> patchCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPatchRequest request
    ) {
        return ApiResponse.success("Category patched successfully", categoryService.patchCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.success("Category deleted successfully", null);
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long categoryId) {
        return ApiResponse.success("Category fetched successfully", categoryService.getCategoryById(categoryId));
    }
}
