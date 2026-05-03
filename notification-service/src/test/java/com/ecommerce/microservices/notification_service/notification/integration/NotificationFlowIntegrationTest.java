package com.ecommerce.microservices.notification_service.notification.integration;

import com.ecommerce.microservices.common.events.payment.PaymentEventTopology;
import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationChannel;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationEntity;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationStatus;
import com.ecommerce.microservices.notification_service.notification.repository.NotificationRepository;
import com.ecommerce.microservices.notification_service.notification.repository.ProcessedNotificationEventRepository;
import com.ecommerce.microservices.notification_service.notification.sender.EmailNotificationSender;
import com.ecommerce.microservices.notification_service.notification.sender.SmsNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
		"spring.flyway.enabled=true",
		"spring.flyway.default-schema=notifications",
		"spring.flyway.schemas=notifications",
		"spring.jpa.properties.hibernate.default_schema=notifications"
})
class NotificationFlowIntegrationTest {

	static {
		System.setProperty("spring.config.location", "classpath:/notification-integration.properties");
	}

	@Container
	static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("notification_service_test")
			.withUsername("postgres")
			.withPassword("postgres");

	@Container
	static final RabbitMQContainer RABBITMQ_CONTAINER = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ProcessedNotificationEventRepository processedNotificationEventRepository;

	@MockBean
	private EmailNotificationSender emailNotificationSender;

	@MockBean
	private SmsNotificationSender smsNotificationSender;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
		registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
		registry.add("spring.rabbitmq.host", RABBITMQ_CONTAINER::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ_CONTAINER::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBITMQ_CONTAINER::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBITMQ_CONTAINER::getAdminPassword);
	}

	@BeforeEach
	void cleanState() {
		notificationRepository.deleteAll();
		processedNotificationEventRepository.deleteAll();
		reset(emailNotificationSender, smsNotificationSender);
	}

	@AfterAll
	static void clearBootOverrides() {
		System.clearProperty("spring.config.location");
	}

	@Test
	void paymentSucceededEventIsConsumedAndPersistedEndToEnd() throws Exception {
		publish(PaymentEventTypes.SUCCEEDED, """
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
				""");

		verify(emailNotificationSender, timeout(5000)).send(
				org.mockito.ArgumentMatchers.eq("onur@example.com"),
				org.mockito.ArgumentMatchers.eq("Odemeniz basariyla alindi"),
				org.mockito.ArgumentMatchers.contains("order-1 numarali siparisiniz")
		);
		verify(smsNotificationSender, timeout(5000)).send(
				"905551112233",
				"Siparis order-1 icin odemeniz alindi. Tutar: 249.9"
		);

		waitUntil(() -> notificationRepository.count() == 2 && processedNotificationEventRepository.count() == 1);

		List<NotificationEntity> notifications = notificationRepository.findAll().stream()
				.sorted(Comparator.comparing(notification -> (String) ReflectionTestUtils.getField(notification, "subject")))
				.toList();

		assertThat(notifications).hasSize(2);
		assertNotification(notifications, NotificationChannel.EMAIL, "onur@example.com", NotificationStatus.SENT, null);
		assertNotification(notifications, NotificationChannel.SMS, "905551112233", NotificationStatus.SENT, null);
		assertThat(processedNotificationEventRepository.existsById("event-1")).isTrue();
	}

	@Test
	void duplicateSucceededEventIsIgnoredAfterFirstProcessing() throws Exception {
		String payload = """
				{
				  "eventId": "event-duplicate",
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
				""";

		publish(PaymentEventTypes.SUCCEEDED, payload);
		waitUntil(() -> processedNotificationEventRepository.existsById("event-duplicate"));
		publish(PaymentEventTypes.SUCCEEDED, payload);
		Thread.sleep(500);

		assertThat(notificationRepository.count()).isEqualTo(2);
		assertThat(processedNotificationEventRepository.count()).isEqualTo(1);
		verify(emailNotificationSender, timeout(5000).times(1)).send(any(), any(), any());
		verify(smsNotificationSender, timeout(5000).times(1)).send(any(), any());
	}

	@Test
	void paymentFailedEventMarksFailedNotificationWhenEmailSenderThrows() throws Exception {
		doThrow(new IllegalStateException("SMTP is down"))
				.when(emailNotificationSender)
				.send(any(), any(), any());

		publish(PaymentEventTypes.FAILED, """
				{
				  "eventId": "event-failed",
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
				""");

		verify(smsNotificationSender, timeout(5000)).send(
				"905551112233",
				"Siparis order-1 icin odeme basarisiz. Sebep: Kart reddedildi"
		);
		waitUntil(() -> notificationRepository.count() == 2 && processedNotificationEventRepository.count() == 1);

		List<NotificationEntity> notifications = notificationRepository.findAll();
		assertNotification(notifications, NotificationChannel.EMAIL, "onur@example.com", NotificationStatus.FAILED, "SMTP is down");
		assertNotification(notifications, NotificationChannel.SMS, "905551112233", NotificationStatus.SENT, null);
	}

	private void publish(String routingKey, String body) {
		rabbitTemplate.send(
				PaymentEventTopology.EXCHANGE,
				routingKey,
				new Message(body.getBytes(), new MessageProperties())
		);
	}

	private void assertNotification(
			List<NotificationEntity> notifications,
			NotificationChannel channel,
			String recipient,
			NotificationStatus status,
			String failureReason
	) {
		NotificationEntity notification = notifications.stream()
				.filter(candidate -> ReflectionTestUtils.getField(candidate, "channel") == channel)
				.findFirst()
				.orElseThrow();

		assertThat(ReflectionTestUtils.getField(notification, "recipient")).isEqualTo(recipient);
		assertThat(ReflectionTestUtils.getField(notification, "status")).isEqualTo(status);
		assertThat(ReflectionTestUtils.getField(notification, "failureReason")).isEqualTo(failureReason);
	}

	private void waitUntil(Callable<Boolean> condition) throws Exception {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
		while (Instant.now().isBefore(deadline)) {
			if (condition.call()) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Condition was not met within timeout");
	}

}
