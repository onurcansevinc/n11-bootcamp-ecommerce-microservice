package com.ecommerce.microservices.notification_service.notification.consumer;

import com.ecommerce.microservices.common.events.payment.PaymentFailedEventPayload;
import com.ecommerce.microservices.common.events.payment.PaymentSucceededEventPayload;
import com.ecommerce.microservices.notification_service.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationPaymentEventConsumerTest {

	@Mock
	private NotificationService notificationService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void consumeDispatchesSucceededEvent() throws Exception {
		NotificationPaymentEventConsumer consumer =
				new NotificationPaymentEventConsumer(objectMapper, notificationService);

		consumer.consume(message("""
				{
				  "eventId": "event-1",
				  "eventType": "payment.succeeded.v1",
				  "payload": {
				    "paymentId": "payment-1",
				    "orderId": "order-1",
				    "customerId": "customer-1",
				    "customerName": "Onur",
				    "customerSurname": "Sevinc",
				    "customerEmail": "onur@example.com",
				    "customerGsmNumber": "905551112233",
				    "provider": "IYZICO",
				    "amount": 249.90
				  }
				}
				"""));

		ArgumentCaptor<PaymentSucceededEventPayload> payloadCaptor =
				ArgumentCaptor.forClass(PaymentSucceededEventPayload.class);
		verify(notificationService).handlePaymentSucceeded(org.mockito.Mockito.eq("event-1"), payloadCaptor.capture());
		assertThat(payloadCaptor.getValue().orderId()).isEqualTo("order-1");
		assertThat(payloadCaptor.getValue().customerEmail()).isEqualTo("onur@example.com");
		assertThat(payloadCaptor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(249.90));
	}

	@Test
	void consumeDispatchesFailedEvent() throws Exception {
		NotificationPaymentEventConsumer consumer =
				new NotificationPaymentEventConsumer(objectMapper, notificationService);

		consumer.consume(message("""
				{
				  "eventId": "event-2",
				  "eventType": "payment.failed.v1",
				  "payload": {
				    "paymentId": "payment-1",
				    "orderId": "order-1",
				    "customerId": "customer-1",
				    "customerName": "Onur",
				    "customerSurname": "Sevinc",
				    "customerEmail": "onur@example.com",
				    "customerGsmNumber": "905551112233",
				    "provider": "IYZICO",
				    "amount": 249.90,
				    "failureReason": "Kart reddedildi"
				  }
				}
				"""));

		ArgumentCaptor<PaymentFailedEventPayload> payloadCaptor =
				ArgumentCaptor.forClass(PaymentFailedEventPayload.class);
		verify(notificationService).handlePaymentFailed(org.mockito.Mockito.eq("event-2"), payloadCaptor.capture());
		assertThat(payloadCaptor.getValue().failureReason()).isEqualTo("Kart reddedildi");
	}

	@Test
	void consumeRejectsMissingEventId() {
		NotificationPaymentEventConsumer consumer =
				new NotificationPaymentEventConsumer(objectMapper, notificationService);

		assertThatThrownBy(() -> consumer.consume(message("""
				{
				  "eventType": "payment.succeeded.v1",
				  "payload": {}
				}
				""")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Missing required notification event field: eventId");

		verifyNoInteractions(notificationService);
	}

	@Test
	void consumeRejectsUnsupportedEventType() {
		NotificationPaymentEventConsumer consumer =
				new NotificationPaymentEventConsumer(objectMapper, notificationService);

		assertThatThrownBy(() -> consumer.consume(message("""
				{
				  "eventId": "event-3",
				  "eventType": "payment.refunded.v1",
				  "payload": {}
				}
				""")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unsupported notification event type: payment.refunded.v1");

		verifyNoInteractions(notificationService);
	}

	private Message message(String json) {
		return new Message(json.getBytes(), new MessageProperties());
	}

}
