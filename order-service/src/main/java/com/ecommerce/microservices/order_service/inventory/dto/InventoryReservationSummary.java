package com.ecommerce.microservices.order_service.inventory.dto;

public record InventoryReservationSummary(
		String reservationCode,
		Long productId,
		Integer quantity,
		String status
) {
}
