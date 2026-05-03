package com.ecommerce.microservices.payment_service.payment.service;

import com.ecommerce.microservices.payment_service.order.client.PaymentOrderClient;
import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventEntity;
import com.ecommerce.microservices.payment_service.outbox.entity.PaymentOutboxEventStatus;
import com.ecommerce.microservices.payment_service.outbox.repository.PaymentOutboxEventRepository;
import com.ecommerce.microservices.payment_service.outbox.service.PaymentOutboxService;
import com.ecommerce.microservices.payment_service.payment.dto.CreatePaymentRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentAddressRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentBuyerRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentCheckoutRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentResponse;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;
import com.ecommerce.microservices.payment_service.payment.exception.DuplicatePendingPaymentException;
import com.ecommerce.microservices.payment_service.payment.gateway.IyzicoPaymentGatewayAdapter;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayAdapter;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayRegistry;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayRequest;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentInitiationResult;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentVerificationResult;
import com.ecommerce.microservices.payment_service.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
		PaymentService.class,
		PaymentOutboxService.class,
		PaymentServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
		"spring.config.import=",
		"spring.cloud.config.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"eureka.client.enabled=false",
		"spring.flyway.enabled=true",
		"spring.flyway.default-schema=payments",
		"spring.flyway.schemas=payments",
		"spring.jpa.properties.hibernate.default_schema=payments"
})
class PaymentServiceIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("payment-service-test")
			.withUsername("test")
			.withPassword("test");

	private final PaymentGatewayAdapter paymentGatewayAdapter = mock(PaymentGatewayAdapter.class);

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PaymentOutboxEventRepository paymentOutboxEventRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PaymentOrderClient paymentOrderClient;

	@MockBean
	private PaymentGatewayRegistry paymentGatewayRegistry;

	@MockBean
	private IyzicoPaymentGatewayAdapter iyzicoPaymentGatewayAdapter;

	@MockBean
	private RabbitTemplate rabbitTemplate;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void createPaymentPersistsPendingPaymentUsingGatewayResult() {
		when(paymentOrderClient.getRequiredOrder("order-1", "Bearer token")).thenReturn(payableOrder());
		when(paymentGatewayRegistry.getRequired(PaymentProvider.IYZICO)).thenReturn(paymentGatewayAdapter);
		when(paymentGatewayAdapter.initiate(any(PaymentGatewayRequest.class)))
				.thenReturn(new PaymentInitiationResult("ext-1", "https://checkout.example.com/ext-1"));

		PaymentResponse response = paymentService.createPayment(
				"customer-1",
				"Bearer token",
				createPaymentRequest("order-1"),
				"127.0.0.1"
		);

		PaymentEntity persistedPayment = paymentRepository.findById(response.id()).orElseThrow();

		assertThat(persistedPayment.getOrderId()).isEqualTo("order-1");
		assertThat(persistedPayment.getCustomerId()).isEqualTo("customer-1");
		assertThat(persistedPayment.getProvider()).isEqualTo(PaymentProvider.IYZICO);
		assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(persistedPayment.getExternalPaymentId()).isEqualTo("ext-1");
		assertThat(persistedPayment.getCheckoutUrl()).isEqualTo("https://checkout.example.com/ext-1");
		assertThat(paymentOutboxEventRepository.count()).isZero();
	}

	@Test
	void createPaymentRejectsDuplicatePendingPaymentWithoutCreatingNewRecord() {
		PaymentEntity existingPayment = paymentRepository.saveAndFlush(new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"existing-ext",
				"https://checkout.example.com/existing-ext",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		));

		when(paymentOrderClient.getRequiredOrder("order-1", "Bearer token")).thenReturn(payableOrder());

		assertThatThrownBy(() -> paymentService.createPayment(
				"customer-1",
				"Bearer token",
				createPaymentRequest("order-1"),
				"127.0.0.1"
		))
				.isInstanceOf(DuplicatePendingPaymentException.class);

		assertThat(paymentRepository.count()).isEqualTo(1);
		assertThat(paymentRepository.findById(existingPayment.getId())).isPresent();
		verify(paymentGatewayRegistry, never()).getRequired(any());
	}

	@Test
	void handleIyzicoCallbackMarksPaymentSucceededAndAppendsSucceededOutboxEvent() throws Exception {
		PaymentEntity payment = paymentRepository.saveAndFlush(new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"token-1",
				"https://checkout.example.com/token-1",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		));

		when(iyzicoPaymentGatewayAdapter.retrieve("token-1")).thenReturn(PaymentVerificationResult.success());

		PaymentResponse response = paymentService.handleIyzicoCallback("token-1");

		PaymentEntity updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
		PaymentOutboxEventEntity outboxEvent = paymentOutboxEventRepository.findAll().getFirst();
		JsonNode payloadRoot = objectMapper.readTree(outboxEvent.getPayload());

		assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(updatedPayment.getCompletedAt()).isNotNull();
		assertThat(paymentOutboxEventRepository.count()).isEqualTo(1);
		assertThat(outboxEvent.getAggregateId()).isEqualTo(payment.getId());
		assertThat(outboxEvent.getEventType()).isEqualTo("payment.succeeded.v1");
		assertThat(outboxEvent.getStatus()).isEqualTo(PaymentOutboxEventStatus.PENDING);
		assertThat(payloadRoot.path("eventType").asText()).isEqualTo("payment.succeeded.v1");
		assertThat(payloadRoot.path("payload").path("orderId").asText()).isEqualTo("order-1");
		assertThat(payloadRoot.path("payload").path("paymentId").asText()).isEqualTo(payment.getId());
	}

	@Test
	void handleIyzicoCallbackMarksPaymentFailedAndAppendsFailedOutboxEvent() throws Exception {
		PaymentEntity payment = paymentRepository.saveAndFlush(new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"token-2",
				"https://checkout.example.com/token-2",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		));

		when(iyzicoPaymentGatewayAdapter.retrieve("token-2"))
				.thenReturn(PaymentVerificationResult.failure("Kart reddedildi"));

		PaymentResponse response = paymentService.handleIyzicoCallback("token-2");

		PaymentEntity updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
		PaymentOutboxEventEntity outboxEvent = paymentOutboxEventRepository.findAll().getFirst();
		JsonNode payloadRoot = objectMapper.readTree(outboxEvent.getPayload());

		assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
		assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(updatedPayment.getFailureReason()).isEqualTo("Kart reddedildi");
		assertThat(updatedPayment.getCompletedAt()).isNotNull();
		assertThat(outboxEvent.getEventType()).isEqualTo("payment.failed.v1");
		assertThat(payloadRoot.path("eventType").asText()).isEqualTo("payment.failed.v1");
		assertThat(payloadRoot.path("payload").path("failureReason").asText()).isEqualTo("Kart reddedildi");
	}

	@Test
	void handleIyzicoCallbackIsIdempotentForAlreadyCompletedPayment() {
		PaymentEntity payment = paymentRepository.saveAndFlush(new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"token-3",
				"https://checkout.example.com/token-3",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		));

		when(iyzicoPaymentGatewayAdapter.retrieve("token-3")).thenReturn(PaymentVerificationResult.success());

		paymentService.handleIyzicoCallback("token-3");
		paymentService.handleIyzicoCallback("token-3");

		assertThat(paymentOutboxEventRepository.count()).isEqualTo(1);
		verify(iyzicoPaymentGatewayAdapter).retrieve("token-3");
	}

	private OrderSummary payableOrder() {
		return new OrderSummary(
				"order-1",
				"customer-1",
				"PENDING_PAYMENT",
				BigDecimal.valueOf(250),
				List.of()
		);
	}

	private CreatePaymentRequest createPaymentRequest(String orderId) {
		PaymentBuyerRequest buyer = new PaymentBuyerRequest(
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233",
				"11111111110",
				"Maslak Mahallesi Buyukdere Caddesi No:1",
				"Istanbul",
				"Turkiye",
				"34000"
		);
		PaymentAddressRequest address = new PaymentAddressRequest(
				"Onur Sevinc",
				"Maslak Mahallesi Buyukdere Caddesi No:1",
				"Istanbul",
				"Turkiye",
				"34000"
		);

		return new CreatePaymentRequest(
				orderId,
				PaymentProvider.IYZICO,
				new PaymentCheckoutRequest("tr", buyer, address, address)
		);
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
