package com.ecommerce.microservices.inventory_service.inventory.controller;

import com.ecommerce.microservices.common.web.response.ApiResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationRequest;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryReservationResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryResponse;
import com.ecommerce.microservices.inventory_service.inventory.dto.InventoryUpsertRequest;
import com.ecommerce.microservices.inventory_service.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory", description = "Inventory stock and reservation endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping("/{productId}")
	@Operation(summary = "Get inventory by product id")
	public ApiResponse<InventoryResponse> getInventoryByProductId(@PathVariable @Positive Long productId) {
		return ApiResponse.success(
				"Inventory fetched successfully",
				inventoryService.getInventoryByProductId(productId)
		);
	}

	@PutMapping("/{productId}")
	@Operation(summary = "Create or replace inventory for a product")
	public ApiResponse<InventoryResponse> upsertInventory(
			@PathVariable @Positive Long productId,
			@Valid @RequestBody InventoryUpsertRequest request
	) {
		return ApiResponse.success(
				"Inventory upserted successfully",
				inventoryService.upsertInventory(productId, request)
		);
	}

	@PostMapping("/reservations")
	@Operation(summary = "Create inventory reservation")
	public ResponseEntity<ApiResponse<InventoryReservationResponse>> createReservation(
			@Valid @RequestBody InventoryReservationRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"Inventory reservation created successfully",
						inventoryService.createReservation(request)
				));
	}

	@GetMapping("/reservations/{reservationCode}")
	@Operation(summary = "Get reservation by code")
	public ApiResponse<InventoryReservationResponse> getReservationByCode(@PathVariable String reservationCode) {
		return ApiResponse.success(
				"Inventory reservation fetched successfully",
				inventoryService.getReservationByCode(reservationCode)
		);
	}

	@PostMapping("/reservations/{reservationCode}/confirm")
	@Operation(summary = "Confirm reservation")
	public ApiResponse<InventoryReservationResponse> confirmReservation(@PathVariable String reservationCode) {
		return ApiResponse.success(
				"Inventory reservation confirmed successfully",
				inventoryService.confirmReservation(reservationCode)
		);
	}

	@PostMapping("/reservations/{reservationCode}/release")
	@Operation(summary = "Release reservation")
	public ApiResponse<InventoryReservationResponse> releaseReservation(@PathVariable String reservationCode) {
		return ApiResponse.success(
				"Inventory reservation released successfully",
				inventoryService.releaseReservation(reservationCode)
		);
	}

}
