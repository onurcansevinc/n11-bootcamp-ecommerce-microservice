package com.ecommerce.microservices.order_service.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
		@NotBlank
		String cartId
) {
}
