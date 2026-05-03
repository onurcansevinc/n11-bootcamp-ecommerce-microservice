package com.ecommerce.microservices.inventory_service.inventory.controller;

import com.ecommerce.microservices.inventory_service.common.exception.GlobalExceptionHandler;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationRequest;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryResponse;
import com.ecommerce.microservices.inventory_service.inventory.entity.ReservationStatus;
import com.ecommerce.microservices.inventory_service.inventory.exception.InsufficientInventoryException;
import com.ecommerce.microservices.inventory_service.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class InventoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private InventoryService inventoryService;

	@Test
	void getInventoryReturnsSuccessResponse() throws Exception {
		InventoryResponse inventoryResponse = new InventoryResponse(
				1L,
				8,
				2,
				10,
				LocalDateTime.now()
		);
		when(inventoryService.getInventoryByProductId(1L)).thenReturn(inventoryResponse);

		mockMvc.perform(get("/api/v1/inventory/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Inventory fetched successfully"))
				.andExpect(jsonPath("$.data.productId").value(1))
				.andExpect(jsonPath("$.data.availableQuantity").value(8))
				.andExpect(jsonPath("$.data.reservedQuantity").value(2))
				.andExpect(jsonPath("$.data.totalQuantity").value(10));
	}

	@Test
	void upsertInventoryReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(put("/api/v1/inventory/1")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "availableQuantity": -1
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.detail").value("Request body validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("availableQuantity"));

		verify(inventoryService, never()).upsertInventory(any(), any());
	}

	@Test
	void createReservationReturnsCreatedResponse() throws Exception {
		InventoryReservationRequest request = new InventoryReservationRequest(1L, 2);
		InventoryReservationResponse response = new InventoryReservationResponse(
				"res-1",
				1L,
				2,
				ReservationStatus.RESERVED,
				Instant.parse("2026-05-03T12:15:00Z"),
				null,
				null
		);
		when(inventoryService.createReservation(request)).thenReturn(response);

		mockMvc.perform(post("/api/v1/inventory/reservations")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Inventory reservation created successfully"))
				.andExpect(jsonPath("$.data.reservationCode").value("res-1"))
				.andExpect(jsonPath("$.data.productId").value(1))
				.andExpect(jsonPath("$.data.quantity").value(2))
				.andExpect(jsonPath("$.data.status").value("RESERVED"));
	}

	@Test
	void createReservationMapsConflictToProblemDetail() throws Exception {
		InventoryReservationRequest request = new InventoryReservationRequest(1L, 3);
		when(inventoryService.createReservation(request))
				.thenThrow(new InsufficientInventoryException(1L, 3, 1));

		mockMvc.perform(post("/api/v1/inventory/reservations")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Insufficient inventory"))
				.andExpect(jsonPath("$.detail").value("Insufficient inventory for product id 1. Requested: 3, available: 1"));
	}

	@Test
	void confirmReservationReturnsSuccessResponse() throws Exception {
		InventoryReservationResponse response = new InventoryReservationResponse(
				"res-1",
				1L,
				2,
				ReservationStatus.CONFIRMED,
				Instant.parse("2026-05-03T12:15:00Z"),
				null,
				null
		);
		when(inventoryService.confirmReservation("res-1")).thenReturn(response);

		mockMvc.perform(post("/api/v1/inventory/reservations/res-1/confirm"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Inventory reservation confirmed successfully"))
				.andExpect(jsonPath("$.data.reservationCode").value("res-1"))
				.andExpect(jsonPath("$.data.status").value("CONFIRMED"));
	}

}
