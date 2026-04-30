package com.ecommerce.microservices.cart_service.cart.exception;

public class CartAccessDeniedException extends RuntimeException {

	public CartAccessDeniedException(String cartId) {
		super("You do not have access to cart " + cartId);
	}

}
