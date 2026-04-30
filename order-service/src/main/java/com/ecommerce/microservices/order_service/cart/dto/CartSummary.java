package com.ecommerce.microservices.order_service.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartSummary(
		String id,
		String customerId,
		String status,
		List<CartItemSummary> items,
		BigDecimal totalAmount
) {
}
