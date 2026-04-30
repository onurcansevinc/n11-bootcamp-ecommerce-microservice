package com.ecommerce.microservices.order_service.order.exception;

public class CartAccessDeniedForOrderException extends RuntimeException {

	public CartAccessDeniedForOrderException(String cartId) {
		super("You do not have access to cart: " + cartId);
	}

}
