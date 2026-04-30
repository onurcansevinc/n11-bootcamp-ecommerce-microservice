package com.ecommerce.microservices.cart_service.cart.dto;

import com.ecommerce.microservices.cart_service.cart.entity.Cart;
import com.ecommerce.microservices.cart_service.cart.entity.CartStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
		String id,
		String customerId,
		CartStatus status,
		List<CartItemResponse> items,
		BigDecimal totalAmount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static CartResponse from(Cart cart) {
		List<CartItemResponse> itemResponses = cart.getItemsOrderedById()
				.stream()
				.map(CartItemResponse::from)
				.toList();

		return new CartResponse(
				cart.getId(),
				cart.getCustomerId(),
				cart.getStatus(),
				itemResponses,
				cart.getTotalAmount(),
				cart.getCreatedAt(),
				cart.getUpdatedAt()
		);
	}
}
