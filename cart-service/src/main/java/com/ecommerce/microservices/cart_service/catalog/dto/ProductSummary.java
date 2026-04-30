package com.ecommerce.microservices.cart_service.catalog.dto;

import java.math.BigDecimal;

public record ProductSummary(
		Long id,
		String name,
		BigDecimal price,
		Boolean active
) {
}
