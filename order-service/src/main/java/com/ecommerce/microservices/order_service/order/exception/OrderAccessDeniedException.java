package com.ecommerce.microservices.order_service.order.exception;

public class OrderAccessDeniedException extends RuntimeException {

	public OrderAccessDeniedException(String orderId) {
		super("You do not have access to order: " + orderId);
	}

}
