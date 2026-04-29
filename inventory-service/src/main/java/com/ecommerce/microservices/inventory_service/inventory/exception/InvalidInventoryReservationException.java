package com.ecommerce.microservices.inventory_service.inventory.exception;

public class InvalidInventoryReservationException extends RuntimeException {

	public InvalidInventoryReservationException(String message) {
		super(message);
	}

}
