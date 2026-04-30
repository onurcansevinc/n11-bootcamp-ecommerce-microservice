package com.ecommerce.microservices.order_service.order.dto;

import com.ecommerce.microservices.order_service.order.entity.OrderEntity;
import com.ecommerce.microservices.order_service.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
		String id,
		String customerId,
		String sourceCartId,
		OrderStatus status,
		List<OrderItemResponse> items,
		BigDecimal totalAmount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static OrderResponse from(OrderEntity order) {
		List<OrderItemResponse> itemResponses = order.getItemsOrderedById()
				.stream()
				.map(OrderItemResponse::from)
				.toList();

		return new OrderResponse(
				order.getId(),
				order.getCustomerId(),
				order.getSourceCartId(),
				order.getStatus(),
				itemResponses,
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getUpdatedAt()
		);
	}
}
