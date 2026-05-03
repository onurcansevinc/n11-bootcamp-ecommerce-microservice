package com.ecommerce.microservices.inventory_service.inventory.service;

import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationRequest;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryUpsertRequest;
import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryItem;
import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryReservation;
import com.ecommerce.microservices.inventory_service.inventory.entity.ReservationStatus;
import com.ecommerce.microservices.inventory_service.inventory.exception.InsufficientInventoryException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InvalidInventoryReservationException;
import com.ecommerce.microservices.inventory_service.inventory.repository.InventoryItemRepository;
import com.ecommerce.microservices.inventory_service.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

	private static final Instant NOW = Instant.parse("2026-05-03T12:00:00Z");

	@Mock
	private InventoryItemRepository inventoryItemRepository;

	@Mock
	private InventoryReservationRepository inventoryReservationRepository;

	private InventoryService inventoryService;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
		inventoryService = new InventoryService(
				inventoryItemRepository,
				inventoryReservationRepository,
				fixedClock,
				Duration.ofMinutes(15)
		);
	}

	@Test
	void upsertInventoryCreatesNewInventoryItemWhenMissing() {
		when(inventoryItemRepository.findById(1L)).thenReturn(Optional.empty());
		when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryResponse response = inventoryService.upsertInventory(1L, new InventoryUpsertRequest(12));

		assertThat(response.productId()).isEqualTo(1L);
		assertThat(response.availableQuantity()).isEqualTo(12);
		assertThat(response.reservedQuantity()).isZero();
		assertThat(response.totalQuantity()).isEqualTo(12);
	}

	@Test
	void createReservationReservesStockAndPersistsReservationWithTtl() {
		InventoryItem inventoryItem = new InventoryItem(1L, 10);
		when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
		when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventoryReservationRepository.save(any(InventoryReservation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryReservationResponse response = inventoryService.createReservation(
				new InventoryReservationRequest(1L, 4)
		);

		assertThat(inventoryItem.getAvailableQuantity()).isEqualTo(6);
		assertThat(inventoryItem.getReservedQuantity()).isEqualTo(4);
		assertThat(response.productId()).isEqualTo(1L);
		assertThat(response.quantity()).isEqualTo(4);
		assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));

		ArgumentCaptor<InventoryReservation> reservationCaptor = ArgumentCaptor.forClass(InventoryReservation.class);
		verify(inventoryReservationRepository).save(reservationCaptor.capture());
		assertThat(reservationCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
	}

	@Test
	void createReservationThrowsWhenInventoryIsInsufficient() {
		InventoryItem inventoryItem = new InventoryItem(1L, 2);
		when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

		assertThatThrownBy(() -> inventoryService.createReservation(new InventoryReservationRequest(1L, 3)))
				.isInstanceOf(InsufficientInventoryException.class)
				.hasMessage("Insufficient inventory for product id 1. Requested: 3, available: 2");

		verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
		verify(inventoryReservationRepository, never()).save(any(InventoryReservation.class));
	}

	@Test
	void confirmReservationConfirmsReservedStock() {
		InventoryItem inventoryItem = new InventoryItem(1L, 10);
		inventoryItem.reserve(4);
		InventoryReservation reservation = new InventoryReservation(
				"res-1",
				1L,
				4,
				NOW.plus(Duration.ofMinutes(15))
		);

		when(inventoryReservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));
		when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
		when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventoryReservationRepository.save(any(InventoryReservation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryReservationResponse response = inventoryService.confirmReservation("res-1");

		assertThat(inventoryItem.getAvailableQuantity()).isEqualTo(6);
		assertThat(inventoryItem.getReservedQuantity()).isZero();
		assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
	}

	@Test
	void confirmReservationRejectsExpiredReservation() {
		InventoryReservation expiredReservation = new InventoryReservation(
				"res-1",
				1L,
				4,
				NOW.minusSeconds(1)
		);
		when(inventoryReservationRepository.findById("res-1")).thenReturn(Optional.of(expiredReservation));

		assertThatThrownBy(() -> inventoryService.confirmReservation("res-1"))
				.isInstanceOf(InvalidInventoryReservationException.class)
				.hasMessage("Reservation res-1 has expired");

		verify(inventoryItemRepository, never()).findById(any());
		verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
	}

	@Test
	void releaseReservationRestoresAvailableQuantityAndMarksReservationReleased() {
		InventoryItem inventoryItem = new InventoryItem(1L, 10);
		inventoryItem.reserve(4);
		InventoryReservation reservation = new InventoryReservation(
				"res-1",
				1L,
				4,
				NOW.plus(Duration.ofMinutes(15))
		);

		when(inventoryReservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));
		when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
		when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventoryReservationRepository.save(any(InventoryReservation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryReservationResponse response = inventoryService.releaseReservation("res-1");

		assertThat(inventoryItem.getAvailableQuantity()).isEqualTo(10);
		assertThat(inventoryItem.getReservedQuantity()).isZero();
		assertThat(response.status()).isEqualTo(ReservationStatus.RELEASED);
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
	}

}
