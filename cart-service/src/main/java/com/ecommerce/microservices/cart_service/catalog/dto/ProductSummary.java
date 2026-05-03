package com.ecommerce.microservices.cart_service.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSummary(
		Long id,
		String name,
		BigDecimal price,
		Boolean active
) {
}
