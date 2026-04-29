package com.ecommerce.microservices.inventory_service.inventory.dto;

import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryItem;

import java.time.LocalDateTime;

public record InventoryResponse(
		Long productId,
		Integer availableQuantity,
		Integer reservedQuantity,
		Integer totalQuantity,
		LocalDateTime updatedAt
) {
	public static InventoryResponse from(InventoryItem inventoryItem) {
		return new InventoryResponse(
				inventoryItem.getProductId(),
				inventoryItem.getAvailableQuantity(),
				inventoryItem.getReservedQuantity(),
				inventoryItem.getTotalQuantity(),
				inventoryItem.getUpdatedAt()
		);
	}
}
