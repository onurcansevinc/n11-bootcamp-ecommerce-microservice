package com.ecommerce.microservices.order_service.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

	public InventoryServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
