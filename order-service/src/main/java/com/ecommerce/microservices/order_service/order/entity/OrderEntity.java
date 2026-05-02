package com.ecommerce.microservices.order_service.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
public class OrderEntity {

	@Id
	private String id;

	@Column(nullable = false, updatable = false)
	private String customerId;

	@Column(nullable = false, updatable = false)
	private String sourceCartId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItemEntity> items = new ArrayList<>();

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected OrderEntity() {
	}

	public OrderEntity(String customerId, String sourceCartId, BigDecimal totalAmount) {
		this.id = UUID.randomUUID().toString();
		this.customerId = customerId;
		this.sourceCartId = sourceCartId;
		this.status = OrderStatus.PENDING_PAYMENT;
		this.totalAmount = totalAmount;
	}

	public void addItem(OrderItemEntity item) {
		item.attachToOrder(this);
		this.items.add(item);
	}

	public void markPaid() {
		this.status = OrderStatus.PAID;
	}

	public void markPaymentFailed() {
		this.status = OrderStatus.PAYMENT_FAILED;
	}

	public List<OrderItemEntity> getItemsOrderedById() {
		return items.stream()
				.sorted(Comparator.comparing(OrderItemEntity::getId, Comparator.nullsLast(Long::compareTo)))
				.toList();
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

	public String getId() {
		return id;
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getSourceCartId() {
		return sourceCartId;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
