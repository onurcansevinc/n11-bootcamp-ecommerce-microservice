package com.ecommerce.microservices.inventory_service.inventory.repository;

import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
}
