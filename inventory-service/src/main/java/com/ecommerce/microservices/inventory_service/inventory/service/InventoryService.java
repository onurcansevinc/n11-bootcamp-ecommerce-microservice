package com.ecommerce.microservices.inventory_service.inventory.service;

import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationRequest;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryUpsertRequest;
import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryItem;
import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryReservation;
import com.ecommerce.microservices.inventory_service.inventory.exception.InsufficientInventoryException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InvalidInventoryReservationException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InventoryItemNotFoundException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InventoryReservationNotFoundException;
import com.ecommerce.microservices.inventory_service.inventory.repository.InventoryItemRepository;
import com.ecommerce.microservices.inventory_service.inventory.repository.InventoryReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryService {

	private final InventoryItemRepository inventoryItemRepository;
	private final InventoryReservationRepository inventoryReservationRepository;
	private final Clock clock;
	private final Duration reservationTtl;

	public InventoryService(
			InventoryItemRepository inventoryItemRepository,
			InventoryReservationRepository inventoryReservationRepository,
			Clock clock,
			@Value("${inventory.reservation.ttl:15m}") Duration reservationTtl
	) {
		this.inventoryItemRepository = inventoryItemRepository;
		this.inventoryReservationRepository = inventoryReservationRepository;
		this.clock = clock;
		this.reservationTtl = reservationTtl;
	}

	@Transactional(readOnly = true)
	public InventoryResponse getInventoryByProductId(Long productId) {
		return InventoryResponse.from(getInventoryItem(productId));
	}

	@Transactional
	public InventoryResponse upsertInventory(Long productId, InventoryUpsertRequest request) {
		InventoryItem inventoryItem = inventoryItemRepository.findById(productId)
				.map(existingInventory -> {
					existingInventory.setAvailableQuantity(request.availableQuantity());
					return existingInventory;
				})
				.orElseGet(() -> new InventoryItem(productId, request.availableQuantity()));

		return InventoryResponse.from(inventoryItemRepository.save(inventoryItem));
	}

	@Transactional
	public InventoryReservationResponse createReservation(InventoryReservationRequest request) {
		InventoryItem inventoryItem = getInventoryItem(request.productId());
		int requestedQuantity = request.quantity();

		if (!inventoryItem.canReserve(requestedQuantity)) {
			throw new InsufficientInventoryException(
					request.productId(),
					requestedQuantity,
					inventoryItem.getAvailableQuantity()
			);
		}

		inventoryItem.reserve(requestedQuantity);
		InventoryReservation reservation = new InventoryReservation(
				UUID.randomUUID().toString(),
				request.productId(),
				requestedQuantity,
				Instant.now(clock).plus(reservationTtl)
		);

		inventoryItemRepository.save(inventoryItem);
		return InventoryReservationResponse.from(inventoryReservationRepository.save(reservation));
	}

	@Transactional(readOnly = true)
	public InventoryReservationResponse getReservationByCode(String reservationCode) {
		return InventoryReservationResponse.from(getReservation(reservationCode));
	}

	@Transactional
	public InventoryReservationResponse confirmReservation(String reservationCode) {
		InventoryReservation reservation = getReservation(reservationCode);
		if (reservation.getStatus() == com.ecommerce.microservices.inventory_service.inventory.entity.ReservationStatus.CONFIRMED) {
			return InventoryReservationResponse.from(reservation);
		}
		validateConfirmableReservation(reservation);

		InventoryItem inventoryItem = getInventoryItem(reservation.getProductId());
		if (inventoryItem.getReservedQuantity() < reservation.getQuantity()) {
			throw new InvalidInventoryReservationException(
					"Reserved quantity is inconsistent for product id " + reservation.getProductId()
			);
		}

		inventoryItem.confirm(reservation.getQuantity());
		reservation.confirm();

		inventoryItemRepository.save(inventoryItem);
		return InventoryReservationResponse.from(inventoryReservationRepository.save(reservation));
	}

	@Transactional
	public InventoryReservationResponse releaseReservation(String reservationCode) {
		InventoryReservation reservation = getReservation(reservationCode);
		if (reservation.getStatus() == com.ecommerce.microservices.inventory_service.inventory.entity.ReservationStatus.RELEASED) {
			return InventoryReservationResponse.from(reservation);
		}
		validateReleasableReservation(reservation);

		InventoryItem inventoryItem = getInventoryItem(reservation.getProductId());
		if (inventoryItem.getReservedQuantity() < reservation.getQuantity()) {
			throw new InvalidInventoryReservationException(
					"Reserved quantity is inconsistent for product id " + reservation.getProductId()
			);
		}

		inventoryItem.release(reservation.getQuantity());
		reservation.release();

		inventoryItemRepository.save(inventoryItem);
		return InventoryReservationResponse.from(inventoryReservationRepository.save(reservation));
	}

	private void validateConfirmableReservation(InventoryReservation reservation) {
		validateReleasableReservation(reservation);

		if (reservation.isExpired(clock)) {
			throw new InvalidInventoryReservationException(
					"Reservation " + reservation.getReservationCode() + " has expired"
			);
		}
	}

	private void validateReleasableReservation(InventoryReservation reservation) {
		if (!reservation.isReserved()) {
			throw new InvalidInventoryReservationException(
					"Reservation " + reservation.getReservationCode() + " is already " + reservation.getStatus()
			);
		}
	}

	private InventoryItem getInventoryItem(Long productId) {
		return inventoryItemRepository.findById(productId)
				.orElseThrow(() -> new InventoryItemNotFoundException(productId));
	}

	private InventoryReservation getReservation(String reservationCode) {
		return inventoryReservationRepository.findById(reservationCode)
				.orElseThrow(() -> new InventoryReservationNotFoundException(reservationCode));
	}

}
