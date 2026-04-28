package com.ecommerce.microservices.product_service.controller;

import com.ecommerce.microservices.product_service.common.response.ApiResponse;
import com.ecommerce.microservices.product_service.dto.ProductResponse;
import com.ecommerce.microservices.product_service.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllProducts() {
        return ApiResponse.success("Products fetched successfully", productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.success("Product fetched successfully", productService.getProductById(productId));
    }
}
