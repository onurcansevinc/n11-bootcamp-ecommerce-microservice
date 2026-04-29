package com.ecommerce.microservices.inventory_service.inventory.exception;

public class InventoryReservationNotFoundException extends RuntimeException {

	public InventoryReservationNotFoundException(String reservationCode) {
		super("Inventory reservation not found for code " + reservationCode);
	}

}
