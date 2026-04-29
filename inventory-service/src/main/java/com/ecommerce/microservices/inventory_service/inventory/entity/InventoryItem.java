package com.ecommerce.microservices.inventory_service.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

	@Id
	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(name = "available_quantity", nullable = false)
	private int availableQuantity;

	@Column(name = "reserved_quantity", nullable = false)
	private int reservedQuantity;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected InventoryItem() {
	}

	public InventoryItem(Long productId, int availableQuantity) {
		this.productId = productId;
		this.availableQuantity = availableQuantity;
		this.reservedQuantity = 0;
	}

	public Long getProductId() {
		return productId;
	}

	public int getAvailableQuantity() {
		return availableQuantity;
	}

	public int getReservedQuantity() {
		return reservedQuantity;
	}

	public Long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public int getTotalQuantity() {
		return availableQuantity + reservedQuantity;
	}

	public boolean canReserve(int quantity) {
		return availableQuantity >= quantity;
	}

	public void setAvailableQuantity(int availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public void reserve(int quantity) {
		this.availableQuantity -= quantity;
		this.reservedQuantity += quantity;
	}

	public void release(int quantity) {
		this.reservedQuantity -= quantity;
		this.availableQuantity += quantity;
	}

	public void confirm(int quantity) {
		this.reservedQuantity -= quantity;
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
