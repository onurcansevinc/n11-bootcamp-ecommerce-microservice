package com.ecommerce.microservices.product_service.category.exception;

public class DuplicateCategorySlugException extends RuntimeException {

    public DuplicateCategorySlugException(String slug) {
        super("Category with slug " + slug + " already exists");
    }
}
