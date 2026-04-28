package com.ecommerce.microservices.product_service.product.exception;

public class DuplicateProductSkuException extends RuntimeException {

    public DuplicateProductSkuException(String sku) {
        super("Product with sku " + sku + " already exists");
    }
}
