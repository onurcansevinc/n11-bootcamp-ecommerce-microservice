package com.ecommerce.microservices.product_service.product.exception;

public class InvalidProductFilterException extends RuntimeException {

    public InvalidProductFilterException(String message) {
        super(message);
    }
}
