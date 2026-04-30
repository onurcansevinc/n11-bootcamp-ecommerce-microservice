package com.ecommerce.microservices.order_service.order.exception;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(String orderId) {
		super("Order not found with id: " + orderId);
	}

}
