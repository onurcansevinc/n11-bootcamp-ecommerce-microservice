package com.ecommerce.microservices.inventory_service.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpsertRequest(
		@NotNull
		@Min(0)
		Integer availableQuantity
) {
}
