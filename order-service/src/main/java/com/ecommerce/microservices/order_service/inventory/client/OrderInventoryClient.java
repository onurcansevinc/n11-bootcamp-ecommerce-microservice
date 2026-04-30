package com.ecommerce.microservices.order_service.inventory.client;

import com.ecommerce.microservices.order_service.inventory.dto.InventoryReservationSummary;

public interface OrderInventoryClient {

	InventoryReservationSummary createReservation(Long productId, Integer quantity, String bearerToken);

	void releaseReservation(String reservationCode, String bearerToken);

}
