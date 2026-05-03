package com.ecommerce.microservices.notification_service.notification.entity;

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
@Table(name = "notifications")
public class NotificationEntity {

	private static final int MAX_FAILURE_REASON_LENGTH = 1000;

	@Id
	private String id;

	@Column(nullable = false, updatable = false)
	private String eventId;

	@Column(nullable = false, updatable = false)
	private String eventType;

	@Column(nullable = false, updatable = false)
	private String orderId;

	@Column(nullable = false, updatable = false)
	private String customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private NotificationChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private NotificationProvider provider;

	@Column(nullable = false, updatable = false)
	private String recipient;

	@Column(nullable = false, updatable = false)
	private String subject;

	@Column(nullable = false, updatable = false, length = 4000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationStatus status;

	@Column(length = MAX_FAILURE_REASON_LENGTH)
	private String failureReason;

	@Column
	private LocalDateTime sentAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected NotificationEntity() {
	}

	public NotificationEntity(
			String eventId,
			String eventType,
			String orderId,
			String customerId,
			NotificationChannel channel,
			NotificationProvider provider,
			String recipient,
			String subject,
			String content
	) {
		this.id = UUID.randomUUID().toString();
		this.eventId = eventId;
		this.eventType = eventType;
		this.orderId = orderId;
		this.customerId = customerId;
		this.channel = channel;
		this.provider = provider;
		this.recipient = recipient;
		this.subject = subject;
		this.content = content;
		this.status = NotificationStatus.PENDING;
	}

	public void markSent() {
		this.status = NotificationStatus.SENT;
		this.failureReason = null;
		this.sentAt = LocalDateTime.now();
	}

	public void markFailed(String failureReason) {
		this.status = NotificationStatus.FAILED;
		this.failureReason = truncateFailureReason(failureReason);
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

	private String truncateFailureReason(String failureReason) {
		if (failureReason == null || failureReason.isBlank()) {
			return "Notification delivery failed";
		}
		if (failureReason.length() <= MAX_FAILURE_REASON_LENGTH) {
			return failureReason;
		}
		return failureReason.substring(0, MAX_FAILURE_REASON_LENGTH);
	}
}
