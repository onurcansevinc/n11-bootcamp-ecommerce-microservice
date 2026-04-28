package com.ecommerce.microservices.product_service.category.exception;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(Long categoryId) {
        super("Category with id " + categoryId + " is in use by one or more products");
    }
}
