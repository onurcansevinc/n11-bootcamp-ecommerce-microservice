package com.ecommerce.microservices.payment_service.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemSummary(
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal
) {
}
