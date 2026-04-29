package com.ecommerce.microservices.inventory_service.inventory.dto;

import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryReservation;
import com.ecommerce.microservices.inventory_service.inventory.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record InventoryReservationResponse(
		String reservationCode,
		Long productId,
		Integer quantity,
		ReservationStatus status,
		Instant expiresAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static InventoryReservationResponse from(InventoryReservation reservation) {
		return new InventoryReservationResponse(
				reservation.getReservationCode(),
				reservation.getProductId(),
				reservation.getQuantity(),
				reservation.getStatus(),
				reservation.getExpiresAt(),
				reservation.getCreatedAt(),
				reservation.getUpdatedAt()
		);
	}
}
