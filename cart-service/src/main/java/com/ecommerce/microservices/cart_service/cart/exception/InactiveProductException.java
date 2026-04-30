package com.ecommerce.microservices.cart_service.cart.exception;

public class InactiveProductException extends RuntimeException {

	public InactiveProductException(Long productId) {
		super("Product with id " + productId + " is not active");
	}

}
