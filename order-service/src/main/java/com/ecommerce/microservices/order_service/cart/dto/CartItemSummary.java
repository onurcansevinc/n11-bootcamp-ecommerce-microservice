package com.ecommerce.microservices.order_service.cart.dto;

import java.math.BigDecimal;

public record CartItemSummary(
		Long id,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal
) {
}
