package com.ecommerce.microservices.order_service.order.dto;

import com.ecommerce.microservices.order_service.order.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
		Long id,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal,
		String reservationCode,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static OrderItemResponse from(OrderItemEntity orderItem) {
		return new OrderItemResponse(
				orderItem.getId(),
				orderItem.getProductId(),
				orderItem.getProductName(),
				orderItem.getUnitPrice(),
				orderItem.getQuantity(),
				orderItem.getLineTotal(),
				orderItem.getReservationCode(),
				orderItem.getCreatedAt(),
				orderItem.getUpdatedAt()
		);
	}
}
