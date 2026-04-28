package com.ecommerce.microservices.product_service.exception;

public class InvalidProductFilterException extends RuntimeException {

    public InvalidProductFilterException(String message) {
        super(message);
    }
}
