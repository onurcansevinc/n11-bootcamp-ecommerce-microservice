package com.ecommerce.microservices.order_service.inventory.client;

import com.ecommerce.microservices.order_service.inventory.dto.InventoryReservationSummary;

public interface OrderInventoryClient {

	InventoryReservationSummary createReservation(Long productId, Integer quantity);

	void confirmReservation(String reservationCode);

	void releaseReservation(String reservationCode);

}
