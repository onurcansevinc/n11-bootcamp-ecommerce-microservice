package com.ecommerce.microservices.order_service.cart.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartSummary(
		String id,
		String customerId,
		String status,
		List<CartItemSummary> items,
		BigDecimal totalAmount
) {
}
