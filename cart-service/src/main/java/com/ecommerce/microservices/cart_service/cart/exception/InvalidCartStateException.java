package com.ecommerce.microservices.cart_service.cart.exception;

public class InvalidCartStateException extends RuntimeException {

	public InvalidCartStateException(String message) {
		super(message);
	}

}
