package com.ecommerce.microservices.payment_service.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox_events")
public class PaymentOutboxEventEntity {

	private static final int MAX_ERROR_LENGTH = 1000;

	@Id
	private String id;

	@Column(nullable = false, updatable = false)
	private String aggregateId;

	@Column(nullable = false, updatable = false)
	private String eventType;

	@Column(nullable = false, updatable = false)
	private String routingKey;

	@Column(nullable = false, updatable = false, columnDefinition = "TEXT")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentOutboxEventStatus status;

	@Column(nullable = false)
	private int attemptCount;

	@Column(length = MAX_ERROR_LENGTH)
	private String lastError;

	@Column
	private LocalDateTime publishedAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected PaymentOutboxEventEntity() {
	}

	public PaymentOutboxEventEntity(String aggregateId, String eventType, String routingKey, String payload) {
		this.id = UUID.randomUUID().toString();
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.routingKey = routingKey;
		this.payload = payload;
		this.status = PaymentOutboxEventStatus.PENDING;
		this.attemptCount = 0;
	}

	public void markPublished() {
		this.status = PaymentOutboxEventStatus.PUBLISHED;
		this.publishedAt = LocalDateTime.now();
		this.lastError = null;
	}

	public void registerPublishFailure(String errorMessage) {
		this.attemptCount++;
		this.lastError = truncate(errorMessage);
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

	public String getAggregateId() {
		return aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public String getPayload() {
		return payload;
	}

	public PaymentOutboxEventStatus getStatus() {
		return status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public String getLastError() {
		return lastError;
	}

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	private String truncate(String errorMessage) {
		if (errorMessage == null || errorMessage.isBlank()) {
			return "RabbitMQ publish failed";
		}
		if (errorMessage.length() <= MAX_ERROR_LENGTH) {
			return errorMessage;
		}
		return errorMessage.substring(0, MAX_ERROR_LENGTH);
	}
}
