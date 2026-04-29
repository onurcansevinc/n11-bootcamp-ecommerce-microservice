package com.ecommerce.microservices.inventory_service.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryReservationRequest(
		@NotNull
		@Positive
		Long productId,
		@NotNull
		@Positive
		Integer quantity
) {
}
