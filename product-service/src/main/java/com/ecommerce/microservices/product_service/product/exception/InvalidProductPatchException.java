package com.ecommerce.microservices.product_service.product.exception;

public class InvalidProductPatchException extends RuntimeException {

    public InvalidProductPatchException(String message) {
        super(message);
    }
}
