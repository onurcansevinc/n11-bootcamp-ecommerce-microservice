package com.ecommerce.microservices.order_service.order.exception;

public class InvalidOrderStateException extends RuntimeException {

	public InvalidOrderStateException(String orderId, String expectedStatus, String actualStatus) {
		super("Order " + orderId + " must be in status " + expectedStatus + " but is " + actualStatus);
	}

}
