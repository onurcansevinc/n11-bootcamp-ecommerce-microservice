package com.ecommerce.microservices.payment_service.outbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxPublisher {

	private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);

	private final PaymentOutboxService paymentOutboxService;

	public PaymentOutboxPublisher(PaymentOutboxService paymentOutboxService) {
		this.paymentOutboxService = paymentOutboxService;
	}

	@Scheduled(fixedDelayString = "${payment.outbox.publish.fixed-delay:5000}")
	public void publishPendingEvents() {
		for (String outboxEventId : paymentOutboxService.findPendingEventIds()) {
			try {
				paymentOutboxService.publishPendingEvent(outboxEventId);
			} catch (RuntimeException exception) {
				log.warn("Payment outbox publish failed for event {}", outboxEventId, exception);
			}
		}
	}

}
