package com.ecommerce.microservices.payment_service.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

	@Id
	private String id;

	@Column(nullable = false, updatable = false)
	private String orderId;

	@Column(nullable = false, updatable = false)
	private String customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private PaymentProvider provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	@Column(nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false, updatable = false)
	private String externalPaymentId;

	@Column(nullable = false, updatable = false, length = 500)
	private String checkoutUrl;

	@Column
	private String failureReason;

	@Column
	private LocalDateTime completedAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected PaymentEntity() {
	}

	public PaymentEntity(
			String orderId,
			String customerId,
			PaymentProvider provider,
			BigDecimal amount,
			String externalPaymentId,
			String checkoutUrl
	) {
		this.id = UUID.randomUUID().toString();
		this.orderId = orderId;
		this.customerId = customerId;
		this.provider = provider;
		this.status = PaymentStatus.PENDING;
		this.amount = amount;
		this.externalPaymentId = externalPaymentId;
		this.checkoutUrl = checkoutUrl;
	}

	public void markSucceeded() {
		this.status = PaymentStatus.SUCCESS;
		this.failureReason = null;
		this.completedAt = LocalDateTime.now();
	}

	public void markFailed(String failureReason) {
		this.status = PaymentStatus.FAILED;
		this.failureReason = failureReason;
		this.completedAt = LocalDateTime.now();
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

	public String getOrderId() {
		return orderId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public PaymentProvider getProvider() {
		return provider;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getExternalPaymentId() {
		return externalPaymentId;
	}

	public String getCheckoutUrl() {
		return checkoutUrl;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
