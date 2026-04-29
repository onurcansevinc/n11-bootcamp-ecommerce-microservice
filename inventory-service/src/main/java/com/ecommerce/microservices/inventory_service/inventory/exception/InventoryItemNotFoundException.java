package com.ecommerce.microservices.inventory_service.inventory.exception;

public class InventoryItemNotFoundException extends RuntimeException {

	public InventoryItemNotFoundException(Long productId) {
		super("Inventory item not found for product id " + productId);
	}

}
