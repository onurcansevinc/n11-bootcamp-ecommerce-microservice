package com.ecommerce.microservices.product_service.category.exception;

public class InvalidCategoryPatchException extends RuntimeException {

    public InvalidCategoryPatchException(String message) {
        super(message);
    }
}
