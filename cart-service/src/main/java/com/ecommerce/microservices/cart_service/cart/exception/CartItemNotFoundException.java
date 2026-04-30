package com.ecommerce.microservices.cart_service.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

	public CartItemNotFoundException(Long itemId, String cartId) {
		super("Cart item with id " + itemId + " was not found in cart " + cartId);
	}

}
