package com.ecommerce.microservices.order_service.order.exception;

public class CartNotFoundForOrderException extends RuntimeException {

	public CartNotFoundForOrderException(String cartId) {
		super("Cart not found with id: " + cartId);
	}

}
