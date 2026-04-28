package com.ecommerce.microservices.product_service.controller;

import com.ecommerce.microservices.product_service.common.response.ApiResponse;
import com.ecommerce.microservices.product_service.common.response.ResponseMeta;
import com.ecommerce.microservices.product_service.dto.ProductResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import com.ecommerce.microservices.product_service.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal maxPrice
    ) {
        Page<ProductResponse> productPage = productService.getAllProducts(
                page,
                size,
                search,
                active,
                categoryId,
                minPrice,
                maxPrice
        );

        return ApiResponse.success(
                "Products fetched successfully",
                productPage.getContent(),
                ResponseMeta.from(productPage)
        );
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.success("Product fetched successfully", productService.getProductById(productId));
    }
}
