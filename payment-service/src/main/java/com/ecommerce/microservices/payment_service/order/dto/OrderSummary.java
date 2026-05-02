package com.ecommerce.microservices.payment_service.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderSummary(
		String id,
		String customerId,
		String status,
		BigDecimal totalAmount,
		List<OrderItemSummary> items
) {
}
