package com.ecommerce.microservices.order_service.order.exception;

public class EmptyCartForOrderException extends RuntimeException {

	public EmptyCartForOrderException(String cartId) {
		super("Cart " + cartId + " has no items to order");
	}

}
