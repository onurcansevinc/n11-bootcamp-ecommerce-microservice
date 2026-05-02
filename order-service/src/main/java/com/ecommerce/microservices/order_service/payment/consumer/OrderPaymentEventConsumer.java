package com.ecommerce.microservices.order_service.payment.consumer;

import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.common.events.payment.PaymentFailedEventPayload;
import com.ecommerce.microservices.common.events.payment.PaymentSucceededEventPayload;
import com.ecommerce.microservices.order_service.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final OrderService orderService;

	public OrderPaymentEventConsumer(
			ObjectMapper objectMapper,
			OrderService orderService
	) {
		this.objectMapper = objectMapper;
		this.orderService = orderService;
	}

	@RabbitListener(queues = "${order.payment.events.queue:order.payment-events.v1}")
	public void consume(Message message) throws Exception {
		JsonNode root = objectMapper.readTree(message.getBody());
		String eventId = requiredText(root, "eventId");
		String eventType = requiredText(root, "eventType");
		JsonNode payloadNode = root.path("payload");

		switch (eventType) {
			case PaymentEventTypes.SUCCEEDED -> {
				PaymentSucceededEventPayload payload = objectMapper.treeToValue(
						payloadNode,
						PaymentSucceededEventPayload.class
				);
				orderService.handlePaymentSucceededEvent(eventId, payload.orderId());
			}
			case PaymentEventTypes.FAILED -> {
				PaymentFailedEventPayload payload = objectMapper.treeToValue(
						payloadNode,
						PaymentFailedEventPayload.class
				);
				orderService.handlePaymentFailedEvent(eventId, payload.orderId());
			}
			default -> throw new IllegalArgumentException("Unsupported payment event type: " + eventType);
		}
	}

	private String requiredText(JsonNode root, String fieldName) {
		String value = root.path(fieldName).asText(null);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing required payment event field: " + fieldName);
		}
		return value;
	}

}
