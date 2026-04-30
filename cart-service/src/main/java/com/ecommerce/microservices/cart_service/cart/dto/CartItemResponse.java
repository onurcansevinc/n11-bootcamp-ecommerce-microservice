package com.ecommerce.microservices.cart_service.cart.dto;

import com.ecommerce.microservices.cart_service.cart.entity.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
		Long id,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static CartItemResponse from(CartItem cartItem) {
		return new CartItemResponse(
				cartItem.getId(),
				cartItem.getProductId(),
				cartItem.getProductName(),
				cartItem.getUnitPrice(),
				cartItem.getQuantity(),
				cartItem.getLineTotal(),
				cartItem.getCreatedAt(),
				cartItem.getUpdatedAt()
		);
	}
}
