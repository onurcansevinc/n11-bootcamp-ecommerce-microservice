package com.ecommerce.microservices.cart_service.cart.exception;

public class ProductNotFoundForCartException extends RuntimeException {

	public ProductNotFoundForCartException(Long productId) {
		super("Product with id " + productId + " was not found");
	}

}
