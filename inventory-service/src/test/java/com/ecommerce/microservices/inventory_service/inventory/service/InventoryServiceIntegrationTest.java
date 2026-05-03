package com.ecommerce.microservices.inventory_service.inventory.service;

import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationRequest;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryUpsertRequest;
import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryItem;
import com.ecommerce.microservices.inventory_service.inventory.entity.InventoryReservation;
import com.ecommerce.microservices.inventory_service.inventory.entity.ReservationStatus;
import com.ecommerce.microservices.inventory_service.inventory.repository.InventoryItemRepository;
import com.ecommerce.microservices.inventory_service.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InventoryService.class, InventoryServiceIntegrationTest.FixedClockConfiguration.class})
@TestPropertySource(properties = {
		"spring.config.import=",
		"spring.cloud.config.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"eureka.client.enabled=false",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.flyway.enabled=true",
		"spring.flyway.default-schema=inventory",
		"spring.flyway.schemas=inventory",
		"spring.jpa.properties.hibernate.default_schema=inventory",
		"inventory.reservation.ttl=15m"
})
class InventoryServiceIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-05-03T12:00:00Z");

	@Container
	static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("inventory_service_test")
			.withUsername("postgres")
			.withPassword("postgres");

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private InventoryItemRepository inventoryItemRepository;

	@Autowired
	private InventoryReservationRepository inventoryReservationRepository;

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
		registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
	}

	@Test
	void createReservationAndConfirmPersistStateTransitions() {
		inventoryService.upsertInventory(1L, new InventoryUpsertRequest(8));

		InventoryReservationResponse createdReservation = inventoryService.createReservation(
				new InventoryReservationRequest(1L, 3)
		);
		InventoryReservationResponse confirmedReservation = inventoryService.confirmReservation(
				createdReservation.reservationCode()
		);

		InventoryItem persistedInventory = inventoryItemRepository.findById(1L).orElseThrow();
		InventoryReservation persistedReservation = inventoryReservationRepository.findById(
				createdReservation.reservationCode()
		).orElseThrow();

		assertThat(createdReservation.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
		assertThat(confirmedReservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(persistedInventory.getAvailableQuantity()).isEqualTo(5);
		assertThat(persistedInventory.getReservedQuantity()).isZero();
		assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
	}

	@Test
	void releaseReservationRestoresPersistedStock() {
		inventoryService.upsertInventory(2L, new InventoryUpsertRequest(6));

		InventoryReservationResponse createdReservation = inventoryService.createReservation(
				new InventoryReservationRequest(2L, 2)
		);
		InventoryReservationResponse releasedReservation = inventoryService.releaseReservation(
				createdReservation.reservationCode()
		);

		InventoryItem persistedInventory = inventoryItemRepository.findById(2L).orElseThrow();
		InventoryReservation persistedReservation = inventoryReservationRepository.findById(
				createdReservation.reservationCode()
		).orElseThrow();

		assertThat(releasedReservation.status()).isEqualTo(ReservationStatus.RELEASED);
		assertThat(persistedInventory.getAvailableQuantity()).isEqualTo(6);
		assertThat(persistedInventory.getReservedQuantity()).isZero();
		assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
	}

	@Test
	void confirmReservationIsIdempotentAfterInitialConfirmation() {
		inventoryService.upsertInventory(3L, new InventoryUpsertRequest(7));

		InventoryReservationResponse createdReservation = inventoryService.createReservation(
				new InventoryReservationRequest(3L, 4)
		);
		inventoryService.confirmReservation(createdReservation.reservationCode());
		InventoryReservationResponse confirmedAgain = inventoryService.confirmReservation(
				createdReservation.reservationCode()
		);

		InventoryItem persistedInventory = inventoryItemRepository.findById(3L).orElseThrow();

		assertThat(confirmedAgain.status()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(persistedInventory.getAvailableQuantity()).isEqualTo(3);
		assertThat(persistedInventory.getReservedQuantity()).isZero();
	}

	@TestConfiguration
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock clock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

	}

}
