package com.ecommerce.microservices.cart_service.cart.exception;

public class CartNotFoundException extends RuntimeException {

	public CartNotFoundException(String cartId) {
		super("Cart with id " + cartId + " was not found");
	}

}
