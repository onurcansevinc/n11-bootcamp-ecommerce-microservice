package com.ecommerce.microservices.order_service.order.exception;

public class CartServiceUnavailableException extends RuntimeException {

	public CartServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
