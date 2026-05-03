package com.ecommerce.microservices.notification_service.notification.consumer;

import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.common.events.payment.PaymentFailedEventPayload;
import com.ecommerce.microservices.common.events.payment.PaymentSucceededEventPayload;
import com.ecommerce.microservices.notification_service.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationPaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationService notificationService;

	public NotificationPaymentEventConsumer(
			ObjectMapper objectMapper,
			NotificationService notificationService
	) {
		this.objectMapper = objectMapper;
		this.notificationService = notificationService;
	}

	@RabbitListener(queues = "${notification.payment.events.queue:notification.payment-events.v1}")
	public void consume(Message message) throws Exception {
		JsonNode root = objectMapper.readTree(message.getBody());
		String eventId = requiredText(root, "eventId");
		String eventType = requiredText(root, "eventType");
		JsonNode payloadNode = root.path("payload");

		switch (eventType) {
			case PaymentEventTypes.SUCCEEDED -> notificationService.handlePaymentSucceeded(
					eventId,
					objectMapper.treeToValue(payloadNode, PaymentSucceededEventPayload.class)
			);
			case PaymentEventTypes.FAILED -> notificationService.handlePaymentFailed(
					eventId,
					objectMapper.treeToValue(payloadNode, PaymentFailedEventPayload.class)
			);
			default -> throw new IllegalArgumentException("Unsupported notification event type: " + eventType);
		}
	}

	private String requiredText(JsonNode root, String fieldName) {
		String value = root.path(fieldName).asText(null);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing required notification event field: " + fieldName);
		}
		return value;
	}

}
