package com.ecommerce.microservices.product_service.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product with id " + productId + " was not found");
    }
}
