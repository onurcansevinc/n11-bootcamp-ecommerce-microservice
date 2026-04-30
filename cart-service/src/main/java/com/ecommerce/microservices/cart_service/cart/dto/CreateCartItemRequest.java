package com.ecommerce.microservices.cart_service.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCartItemRequest(
		@NotNull
		@Positive
		Long productId,
		@NotNull
		@Positive
		Integer quantity
) {
}
