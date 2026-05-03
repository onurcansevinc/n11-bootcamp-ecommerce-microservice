package com.ecommerce.microservices.order_service.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReservationSummary(
		String reservationCode,
		Long productId,
		Integer quantity,
		String status
) {
}
