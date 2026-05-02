package com.ecommerce.microservices.order_service.payment.processed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_payment_events")
public class ProcessedPaymentEventEntity {

	@Id
	@Column(nullable = false, updatable = false)
	private String eventId;

	@Column(nullable = false, updatable = false)
	private String eventType;

	@Column(nullable = false, updatable = false)
	private String orderId;

	@Column(nullable = false, updatable = false)
	private LocalDateTime processedAt;

	protected ProcessedPaymentEventEntity() {
	}

	public ProcessedPaymentEventEntity(String eventId, String eventType, String orderId) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.orderId = orderId;
	}

	@PrePersist
	void onCreate() {
		this.processedAt = LocalDateTime.now();
	}
}
