package com.ecommerce.microservices.payment_service.outbox.service;

import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventEntity;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventStatus;
import com.ecommerce.microservices.payment_service.outbox.repository.PaymentOutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
		PaymentOutboxService.class,
		PaymentOutboxPublisher.class,
		PaymentOutboxServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
		"spring.config.import=",
		"spring.cloud.config.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"eureka.client.enabled=false",
		"spring.flyway.enabled=true",
		"spring.flyway.default-schema=payments",
		"spring.flyway.schemas=payments",
		"spring.jpa.properties.hibernate.default_schema=payments",
		"payment.events.exchange=payment.events.test",
		"payment.outbox.publish.batch-size=2"
})
class PaymentOutboxServiceIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("payment-service-test")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	private PaymentOutboxService paymentOutboxService;

	@Autowired
	private PaymentOutboxPublisher paymentOutboxPublisher;

	@Autowired
	private PaymentOutboxEventRepository paymentOutboxEventRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private RabbitTemplate rabbitTemplate;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@BeforeEach
	void cleanDatabase() {
		paymentOutboxEventRepository.deleteAll();
	}

	@Test
	void publishPendingEventMarksOutboxRowPublishedAndSendsRabbitMessage() throws Exception {
		PaymentOutboxEventEntity outboxEvent = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity(
						"payment-1",
						"payment.succeeded.v1",
						"payment.succeeded.v1",
						"""
						{"eventId":"event-1","eventType":"payment.succeeded.v1","payload":{"orderId":"order-1"}}
						"""
				)
		);

		paymentOutboxService.publishPendingEvent(outboxEvent.getId());

		PaymentOutboxEventEntity updatedOutboxEvent = paymentOutboxEventRepository.findById(outboxEvent.getId()).orElseThrow();
		org.mockito.ArgumentCaptor<String> exchangeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.ArgumentCaptor<String> routingKeyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.ArgumentCaptor<Message> messageCaptor = org.mockito.ArgumentCaptor.forClass(Message.class);
		verify(rabbitTemplate).send(exchangeCaptor.capture(), routingKeyCaptor.capture(), messageCaptor.capture());
		JsonNode payloadRoot = objectMapper.readTree(messageCaptor.getValue().getBody());

		assertThat(exchangeCaptor.getValue()).isEqualTo("payment.events.test");
		assertThat(routingKeyCaptor.getValue()).isEqualTo("payment.succeeded.v1");
		assertThat(messageCaptor.getValue().getMessageProperties().getMessageId()).isEqualTo(outboxEvent.getId());
		assertThat(messageCaptor.getValue().getMessageProperties().getHeaders().get("eventType"))
				.isEqualTo("payment.succeeded.v1");
		assertThat(new String(messageCaptor.getValue().getBody(), StandardCharsets.UTF_8))
				.contains("\"eventType\":\"payment.succeeded.v1\"");
		assertThat(payloadRoot.path("payload").path("orderId").asText()).isEqualTo("order-1");

		assertThat(updatedOutboxEvent.getStatus()).isEqualTo(PaymentOutboxEventStatus.PUBLISHED);
		assertThat(updatedOutboxEvent.getPublishedAt()).isNotNull();
		assertThat(updatedOutboxEvent.getAttemptCount()).isZero();
		assertThat(updatedOutboxEvent.getLastError()).isNull();
	}

	@Test
	void publishPendingEventKeepsRowPendingAndStoresErrorWhenRabbitSendFails() {
		PaymentOutboxEventEntity outboxEvent = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity(
						"payment-2",
						"payment.failed.v1",
						"payment.failed.v1",
						"""
						{"eventId":"event-2","eventType":"payment.failed.v1","payload":{"orderId":"order-2"}}
						"""
				)
		);
		doThrow(new IllegalStateException("Broker unavailable"))
				.when(rabbitTemplate)
				.send(anyString(), anyString(), any(Message.class));

		paymentOutboxService.publishPendingEvent(outboxEvent.getId());

		PaymentOutboxEventEntity updatedOutboxEvent = paymentOutboxEventRepository.findById(outboxEvent.getId()).orElseThrow();

		assertThat(updatedOutboxEvent.getStatus()).isEqualTo(PaymentOutboxEventStatus.PENDING);
		assertThat(updatedOutboxEvent.getAttemptCount()).isEqualTo(1);
		assertThat(updatedOutboxEvent.getLastError()).isEqualTo("Broker unavailable");
		assertThat(updatedOutboxEvent.getPublishedAt()).isNull();
	}

	@Test
	void findPendingEventIdsReturnsOldestPendingEventsRespectingBatchSize() throws Exception {
		PaymentOutboxEventEntity firstPending = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity("payment-1", "payment.succeeded.v1", "payment.succeeded.v1", "{}")
		);
		Thread.sleep(5L);
		PaymentOutboxEventEntity secondPending = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity("payment-2", "payment.failed.v1", "payment.failed.v1", "{}")
		);
		Thread.sleep(5L);
		paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity("payment-3", "payment.succeeded.v1", "payment.succeeded.v1", "{}")
		);
		Thread.sleep(5L);
		PaymentOutboxEventEntity publishedEvent = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity("payment-4", "payment.succeeded.v1", "payment.succeeded.v1", "{}")
		);
		publishedEvent.markPublished();
		paymentOutboxEventRepository.saveAndFlush(publishedEvent);

		List<String> pendingEventIds = paymentOutboxService.findPendingEventIds();

		assertThat(pendingEventIds)
				.hasSize(2)
				.containsExactly(firstPending.getId(), secondPending.getId());
	}

	@Test
	void paymentOutboxPublisherContinuesPublishingRemainingEventsAfterOneFailure() throws Exception {
		PaymentOutboxEventEntity firstPending = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity("payment-10", "payment.succeeded.v1", "payment.succeeded.v1", "{}")
		);
		Thread.sleep(5L);
		PaymentOutboxEventEntity secondPending = paymentOutboxEventRepository.saveAndFlush(
				new PaymentOutboxEventEntity("payment-11", "payment.failed.v1", "payment.failed.v1", "{}")
		);

		doThrow(new RuntimeException("First publish failed"))
				.doNothing()
				.when(rabbitTemplate)
				.send(anyString(), anyString(), any(Message.class));

		paymentOutboxPublisher.publishPendingEvents();

		PaymentOutboxEventEntity firstUpdated = paymentOutboxEventRepository.findById(firstPending.getId()).orElseThrow();
		PaymentOutboxEventEntity secondUpdated = paymentOutboxEventRepository.findById(secondPending.getId()).orElseThrow();

		verify(rabbitTemplate, times(2)).send(anyString(), anyString(), any(Message.class));
		assertThat(firstUpdated.getStatus()).isEqualTo(PaymentOutboxEventStatus.PENDING);
		assertThat(firstUpdated.getAttemptCount()).isEqualTo(1);
		assertThat(firstUpdated.getLastError()).isEqualTo("First publish failed");
		assertThat(secondUpdated.getStatus()).isEqualTo(PaymentOutboxEventStatus.PUBLISHED);
		assertThat(secondUpdated.getPublishedAt()).isNotNull();
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		ObjectMapper objectMapper() {
			return JsonMapper.builder()
					.findAndAddModules()
					.build();
		}

	}

}
