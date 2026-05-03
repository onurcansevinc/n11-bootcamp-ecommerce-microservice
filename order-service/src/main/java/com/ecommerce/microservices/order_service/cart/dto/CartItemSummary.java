package com.ecommerce.microservices.order_service.cart.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartItemSummary(
		Long id,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal
) {
}
