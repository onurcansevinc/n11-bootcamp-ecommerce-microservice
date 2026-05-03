package com.ecommerce.microservices.notification_service.notification.integration;

import com.ecommerce.microservices.notification_service.NotificationServiceApplication;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationChannel;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationEntity;
import com.ecommerce.microservices.notification_service.notification.entity.NotificationStatus;
import com.ecommerce.microservices.notification_service.notification.repository.NotificationRepository;
import com.ecommerce.microservices.notification_service.notification.repository.ProcessedNotificationEventRepository;
import com.ecommerce.microservices.notification_service.notification.sender.EmailNotificationSender;
import com.ecommerce.microservices.notification_service.notification.sender.SmsNotificationSender;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventEntity;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventStatus;
import com.ecommerce.microservices.payment_service.outbox.repository.PaymentOutboxEventRepository;
import com.ecommerce.microservices.payment_service.outbox.service.PaymentOutboxPublisher;
import com.ecommerce.microservices.payment_service.outbox.service.PaymentOutboxService;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(
		classes = PaymentOutboxToNotificationIntegrationTest.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@TestPropertySource(properties = {
		"spring.config.import=",
		"spring.cloud.config.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"eureka.client.enabled=false",
		"spring.flyway.enabled=false",
		"payment.events.exchange=payment.events",
		"notification.payment.events.exchange=payment.events"
})
class PaymentOutboxToNotificationIntegrationTest {

	static {
		System.setProperty("spring.config.location", "classpath:/notification-integration.properties");
	}

	private static final String CREATE_PAYMENT_OUTBOX_EVENTS_TABLE = """
			CREATE TABLE IF NOT EXISTS notifications.payment_outbox_events (
			    id VARCHAR(36) PRIMARY KEY,
			    aggregate_id VARCHAR(36) NOT NULL,
			    event_type VARCHAR(100) NOT NULL,
			    routing_key VARCHAR(100) NOT NULL,
			    payload TEXT NOT NULL,
			    status VARCHAR(50) NOT NULL,
			    attempt_count INTEGER NOT NULL DEFAULT 0,
			    last_error VARCHAR(1000),
			    published_at TIMESTAMP,
			    created_at TIMESTAMP NOT NULL,
			    updated_at TIMESTAMP NOT NULL
			)
			""";

	private static final String CREATE_NOTIFICATIONS_TABLE = """
			CREATE TABLE IF NOT EXISTS notifications.notifications (
			    id VARCHAR(36) PRIMARY KEY,
			    event_id VARCHAR(36) NOT NULL,
			    event_type VARCHAR(100) NOT NULL,
			    order_id VARCHAR(36) NOT NULL,
			    customer_id VARCHAR(255) NOT NULL,
			    channel VARCHAR(20) NOT NULL,
			    provider VARCHAR(20) NOT NULL,
			    recipient VARCHAR(255) NOT NULL,
			    subject VARCHAR(255) NOT NULL,
			    content VARCHAR(4000) NOT NULL,
			    status VARCHAR(20) NOT NULL,
			    failure_reason VARCHAR(1000),
			    sent_at TIMESTAMP,
			    created_at TIMESTAMP NOT NULL,
			    updated_at TIMESTAMP NOT NULL
			)
			""";

	private static final String CREATE_PROCESSED_NOTIFICATION_EVENTS_TABLE = """
			CREATE TABLE IF NOT EXISTS notifications.processed_notification_events (
			    event_id VARCHAR(36) PRIMARY KEY,
			    event_type VARCHAR(100) NOT NULL,
			    order_id VARCHAR(36) NOT NULL,
			    processed_at TIMESTAMP NOT NULL
			)
			""";

	@Container
	static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("notification_outbox_chain_test")
			.withUsername("postgres")
			.withPassword("postgres");

	@Container
	static final RabbitMQContainer RABBITMQ_CONTAINER = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PaymentOutboxService paymentOutboxService;

	@Autowired
	private PaymentOutboxPublisher paymentOutboxPublisher;

	@Autowired
	private PaymentOutboxEventRepository paymentOutboxEventRepository;

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
	void prepareDatabase() {
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS notifications");
		jdbcTemplate.execute(CREATE_PAYMENT_OUTBOX_EVENTS_TABLE);
		jdbcTemplate.execute(CREATE_NOTIFICATIONS_TABLE);
		jdbcTemplate.execute(CREATE_PROCESSED_NOTIFICATION_EVENTS_TABLE);
		processedNotificationEventRepository.deleteAll();
		notificationRepository.deleteAll();
		paymentOutboxEventRepository.deleteAll();
		reset(emailNotificationSender, smsNotificationSender);
	}

	@AfterAll
	static void clearBootOverrides() {
		System.clearProperty("spring.config.location");
	}

	@Test
	void paymentOutboxPublisherPublishesSucceededEventThatNotificationConsumerProcesses() throws Exception {
		PaymentEntity payment = new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				new BigDecimal("249.90"),
				"ext-payment-1",
				"https://checkout.test/order-1",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		);
		payment.markSucceeded();

		paymentOutboxService.appendSucceededEvent(payment);
		PaymentOutboxEventEntity pendingOutboxEvent = paymentOutboxEventRepository.findAll().getFirst();

		paymentOutboxPublisher.publishPendingEvents();

		verify(emailNotificationSender, timeout(5000)).send(
				eq("onur@example.com"),
				eq("Odemeniz basariyla alindi"),
				contains("order-1 numarali siparisiniz")
		);
		verify(smsNotificationSender, timeout(5000)).send(
				eq("905551112233"),
				contains("Siparis order-1 icin odemeniz alindi")
		);
		waitUntil(() -> processedNotificationEventRepository.existsById(eventIdOf(pendingOutboxEvent)));
		waitUntil(() -> notificationRepository.count() == 2);

		PaymentOutboxEventEntity publishedOutboxEvent = paymentOutboxEventRepository.findById(pendingOutboxEvent.getId()).orElseThrow();
		List<NotificationEntity> notifications = notificationRepository.findAll().stream()
				.sorted(Comparator.comparing(notification -> (String) ReflectionTestUtils.getField(notification, "subject")))
				.toList();

		assertThat(publishedOutboxEvent.getStatus()).isEqualTo(PaymentOutboxEventStatus.PUBLISHED);
		assertThat(publishedOutboxEvent.getPublishedAt()).isNotNull();
		assertThat(processedNotificationEventRepository.existsById(eventIdOf(publishedOutboxEvent))).isTrue();
		assertNotification(notifications, NotificationChannel.EMAIL, "onur@example.com", NotificationStatus.SENT, null);
		assertNotification(notifications, NotificationChannel.SMS, "905551112233", NotificationStatus.SENT, null);
	}

	@Test
	void paymentOutboxPublisherPublishesFailedEventThatNotificationConsumerProcesses() throws Exception {
		PaymentEntity payment = new PaymentEntity(
				"order-2",
				"customer-2",
				PaymentProvider.IYZICO,
				new BigDecimal("149.50"),
				"ext-payment-2",
				"https://checkout.test/order-2",
				"Aylin",
				"Demir",
				"aylin@example.com",
				"905552223344"
		);
		payment.markFailed("Kart reddedildi");

		paymentOutboxService.appendFailedEvent(payment);
		PaymentOutboxEventEntity pendingOutboxEvent = paymentOutboxEventRepository.findAll().getFirst();

		paymentOutboxPublisher.publishPendingEvents();

		verify(emailNotificationSender, timeout(5000)).send(
				eq("aylin@example.com"),
				eq("Odemeniz basarisiz oldu"),
				contains("Kart reddedildi")
		);
		verify(smsNotificationSender, timeout(5000)).send(
				eq("905552223344"),
				contains("Kart reddedildi")
		);
		waitUntil(() -> processedNotificationEventRepository.existsById(eventIdOf(pendingOutboxEvent)));
		waitUntil(() -> notificationRepository.count() == 2);

		PaymentOutboxEventEntity publishedOutboxEvent = paymentOutboxEventRepository.findById(pendingOutboxEvent.getId()).orElseThrow();
		List<NotificationEntity> notifications = notificationRepository.findAll();

		assertThat(publishedOutboxEvent.getStatus()).isEqualTo(PaymentOutboxEventStatus.PUBLISHED);
		assertThat(publishedOutboxEvent.getPublishedAt()).isNotNull();
		assertThat(processedNotificationEventRepository.existsById(eventIdOf(publishedOutboxEvent))).isTrue();
		assertNotification(notifications, NotificationChannel.EMAIL, "aylin@example.com", NotificationStatus.SENT, null);
		assertNotification(notifications, NotificationChannel.SMS, "905552223344", NotificationStatus.SENT, null);
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

	private String eventIdOf(PaymentOutboxEventEntity outboxEvent) {
		return (String) ReflectionTestUtils.getField(outboxEvent, "id") == null
				? null
				: extractEventId(outboxEvent.getPayload());
	}

	private String extractEventId(String payload) {
		int eventIdKeyIndex = payload.indexOf("\"eventId\":\"");
		int valueStartIndex = eventIdKeyIndex + "\"eventId\":\"".length();
		int valueEndIndex = payload.indexOf('"', valueStartIndex);
		return payload.substring(valueStartIndex, valueEndIndex);
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

	@EnableAutoConfiguration
	@ComponentScan(basePackageClasses = NotificationServiceApplication.class)
	@EntityScan(basePackageClasses = {
			com.ecommerce.microservices.notification_service.notification.entity.NotificationEntity.class,
			com.ecommerce.microservices.notification_service.notification.entity.ProcessedNotificationEventEntity.class,
			com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			com.ecommerce.microservices.notification_service.notification.repository.NotificationRepository.class,
			com.ecommerce.microservices.notification_service.notification.repository.ProcessedNotificationEventRepository.class,
			com.ecommerce.microservices.payment_service.outbox.repository.PaymentOutboxEventRepository.class
	})
	@Import({PaymentOutboxService.class, PaymentOutboxPublisher.class})
	static class TestApplication {
	}

}
