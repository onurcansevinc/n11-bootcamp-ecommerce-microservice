package com.ecommerce.microservices.order_service.order.exception;

public class InventoryReservationFailedException extends RuntimeException {

	public InventoryReservationFailedException(String message) {
		super(message);
	}

}
