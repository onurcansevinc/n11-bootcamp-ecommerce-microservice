package com.ecommerce.microservices.cart_service.cart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(
		name = "carts",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_carts_customer_status", columnNames = {"customer_id", "status"})
		},
		indexes = {
				@Index(name = "idx_carts_customer_id", columnList = "customer_id"),
				@Index(name = "idx_carts_customer_status", columnList = "customer_id,status")
		}
)
public class Cart {

	@Id
	@Column(nullable = false, updatable = false, length = 36)
	private String id;

	@Column(name = "customer_id", nullable = false, updatable = false, length = 120)
	private String customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CartStatus status;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private final List<CartItem> items = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Cart() {
	}

	public Cart(String customerId) {
		this.id = UUID.randomUUID().toString();
		this.customerId = customerId;
		this.status = CartStatus.ACTIVE;
	}

	public String getId() {
		return id;
	}

	public String getCustomerId() {
		return customerId;
	}

	public CartStatus getStatus() {
		return status;
	}

	public List<CartItem> getItems() {
		return items;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public Optional<CartItem> findItemById(Long itemId) {
		return items.stream()
				.filter(item -> item.getId() != null && item.getId().equals(itemId))
				.findFirst();
	}

	public Optional<CartItem> findItemByProductId(Long productId) {
		return items.stream()
				.filter(item -> item.getProductId().equals(productId))
				.findFirst();
	}

	public CartItem addOrIncrementItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
		Optional<CartItem> existingItem = findItemByProductId(productId);
		if (existingItem.isPresent()) {
			CartItem item = existingItem.get();
			item.refreshSnapshot(productName, unitPrice);
			item.incrementQuantity(quantity);
			return item;
		}

		CartItem cartItem = new CartItem(this, productId, productName, unitPrice, quantity);
		items.add(cartItem);
		return cartItem;
	}

	public void changeItemQuantity(Long itemId, int quantity) {
		findItemById(itemId)
				.orElseThrow()
				.changeQuantity(quantity);
	}

	public void removeItem(Long itemId) {
		items.removeIf(item -> item.getId() != null && item.getId().equals(itemId));
	}

	public BigDecimal getTotalAmount() {
		return items.stream()
				.map(CartItem::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public List<CartItem> getItemsOrderedById() {
		return items.stream()
				.sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
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

}
