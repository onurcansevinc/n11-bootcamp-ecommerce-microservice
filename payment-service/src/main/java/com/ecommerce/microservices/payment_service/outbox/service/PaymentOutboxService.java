package com.ecommerce.microservices.payment_service.outbox.service;

import com.ecommerce.microservices.common.events.payment.EventEnvelope;
import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.common.events.payment.PaymentFailedEventPayload;
import com.ecommerce.microservices.common.events.payment.PaymentSucceededEventPayload;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventEntity;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventStatus;
import com.ecommerce.microservices.payment_service.outbox.repository.PaymentOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentOutboxService {

	private final PaymentOutboxEventRepository paymentOutboxEventRepository;
	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final String exchangeName;
	private final int batchSize;

	public PaymentOutboxService(
			PaymentOutboxEventRepository paymentOutboxEventRepository,
			RabbitTemplate rabbitTemplate,
			ObjectMapper objectMapper,
			@Value("${payment.events.exchange:payment.events}") String exchangeName,
			@Value("${payment.outbox.publish.batch-size:50}") int batchSize
	) {
		this.paymentOutboxEventRepository = paymentOutboxEventRepository;
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.exchangeName = exchangeName;
		this.batchSize = batchSize;
	}

	public void appendSucceededEvent(PaymentEntity payment) {
		appendEvent(
				payment,
				PaymentEventTypes.SUCCEEDED,
				new PaymentSucceededEventPayload(
						payment.getId(),
						payment.getOrderId(),
						payment.getCustomerId(),
						payment.getBuyerName(),
						payment.getBuyerSurname(),
						payment.getBuyerEmail(),
						payment.getBuyerGsmNumber(),
						payment.getProvider().name(),
						payment.getAmount()
				)
		);
	}

	public void appendFailedEvent(PaymentEntity payment) {
		appendEvent(
				payment,
				PaymentEventTypes.FAILED,
				new PaymentFailedEventPayload(
						payment.getId(),
						payment.getOrderId(),
						payment.getCustomerId(),
						payment.getBuyerName(),
						payment.getBuyerSurname(),
						payment.getBuyerEmail(),
						payment.getBuyerGsmNumber(),
						payment.getProvider().name(),
						payment.getAmount(),
						payment.getFailureReason()
				)
		);
	}

	@Transactional(readOnly = true)
	public List<String> findPendingEventIds() {
		return paymentOutboxEventRepository
				.findByStatusOrderByCreatedAtAsc(
						PaymentOutboxEventStatus.PENDING,
						PageRequest.of(0, batchSize)
				)
				.stream()
				.map(PaymentOutboxEventEntity::getId)
				.toList();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void publishPendingEvent(String outboxEventId) {
		PaymentOutboxEventEntity outboxEvent = paymentOutboxEventRepository.findById(outboxEventId)
				.orElseThrow();

		if (outboxEvent.getStatus() != PaymentOutboxEventStatus.PENDING) {
			return;
		}

		try {
			rabbitTemplate.send(
					exchangeName,
					outboxEvent.getRoutingKey(),
					MessageBuilder.withBody(outboxEvent.getPayload().getBytes(StandardCharsets.UTF_8))
							.setContentType(MessageProperties.CONTENT_TYPE_JSON)
							.setMessageId(outboxEvent.getId())
							.setHeader("eventType", outboxEvent.getEventType())
							.build()
			);
			outboxEvent.markPublished();
		} catch (RuntimeException exception) {
			outboxEvent.registerPublishFailure(exception.getMessage());
		}
	}

	private void appendEvent(PaymentEntity payment, String eventType, Object payload) {
		try {
			String serializedEvent = objectMapper.writeValueAsString(
					new EventEnvelope<>(
							UUID.randomUUID().toString(),
							eventType,
							PaymentEventTypes.VERSION,
							Instant.now(),
							payload
					)
			);
			paymentOutboxEventRepository.save(
					new PaymentOutboxEventEntity(payment.getId(), eventType, eventType, serializedEvent)
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Payment event could not be serialized for outbox persistence", exception);
		}
	}

}
