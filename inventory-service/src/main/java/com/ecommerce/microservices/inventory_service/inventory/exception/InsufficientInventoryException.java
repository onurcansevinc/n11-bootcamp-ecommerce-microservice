package com.ecommerce.microservices.inventory_service.inventory.exception;

public class InsufficientInventoryException extends RuntimeException {

	public InsufficientInventoryException(Long productId, int requestedQuantity, int availableQuantity) {
		super(
				"Insufficient inventory for product id " + productId
						+ ". Requested: " + requestedQuantity
						+ ", available: " + availableQuantity
		);
	}

}
