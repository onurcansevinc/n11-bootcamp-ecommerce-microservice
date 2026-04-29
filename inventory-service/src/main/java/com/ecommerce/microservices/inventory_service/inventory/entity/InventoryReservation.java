package com.ecommerce.microservices.inventory_service.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "inventory_reservations",
		indexes = {
				@Index(name = "idx_inventory_reservations_product_id", columnList = "product_id"),
				@Index(name = "idx_inventory_reservations_status", columnList = "status"),
				@Index(name = "idx_inventory_reservations_expires_at", columnList = "expires_at")
		}
)
public class InventoryReservation {

	@Id
	@Column(name = "reservation_code", nullable = false, updatable = false, length = 36)
	private String reservationCode;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(nullable = false)
	private int quantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected InventoryReservation() {
	}

	public InventoryReservation(String reservationCode, Long productId, int quantity, Instant expiresAt) {
		this.reservationCode = reservationCode;
		this.productId = productId;
		this.quantity = quantity;
		this.status = ReservationStatus.RESERVED;
		this.expiresAt = expiresAt;
	}

	public String getReservationCode() {
		return reservationCode;
	}

	public Long getProductId() {
		return productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public boolean isReserved() {
		return status == ReservationStatus.RESERVED;
	}

	public boolean isExpired(Clock clock) {
		return !expiresAt.isAfter(clock.instant());
	}

	public void confirm() {
		this.status = ReservationStatus.CONFIRMED;
	}

	public void release() {
		this.status = ReservationStatus.RELEASED;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

}
