package com.ecommerce.microservices.product_service.product.controller;

import com.ecommerce.microservices.common.web.response.ApiResponse;
import com.ecommerce.microservices.common.web.response.ResponseMeta;
import com.ecommerce.microservices.product_service.product.dto.ProductPatchRequest;
import com.ecommerce.microservices.product_service.product.dto.ProductResponse;
import com.ecommerce.microservices.product_service.product.dto.ProductUpsertRequest;
import com.ecommerce.microservices.product_service.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Products", description = "Product catalog endpoints")
@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products")
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

    @PostMapping
    @Operation(summary = "Create product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", productService.createProduct(request)));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Replace product", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpsertRequest request
    ) {
        return ApiResponse.success("Product updated successfully", productService.updateProduct(productId, request));
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Patch product", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ProductResponse> patchProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductPatchRequest request
    ) {
        return ApiResponse.success("Product patched successfully", productService.patchProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ApiResponse.success("Product deleted successfully", null);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product by id")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.success("Product fetched successfully", productService.getProductById(productId));
    }
}
