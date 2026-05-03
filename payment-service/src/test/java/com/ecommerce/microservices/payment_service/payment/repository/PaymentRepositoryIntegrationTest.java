package com.ecommerce.microservices.payment_service.payment.repository;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
class PaymentRepositoryIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("payment-service-test")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	private PaymentRepository paymentRepository;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void existsByOrderIdAndStatusInFindsPendingPayments() {
		PaymentEntity payment = new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"ext-1",
				"https://checkout.example.com/ext-1",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		);
		paymentRepository.saveAndFlush(payment);

		boolean exists = paymentRepository.existsByOrderIdAndStatusIn(
				"order-1",
				List.of(PaymentStatus.PENDING, PaymentStatus.SUCCESS)
		);

		assertThat(exists).isTrue();
	}

	@Test
	void findByExternalPaymentIdAndProviderReturnsMatchingPayment() {
		PaymentEntity payment = new PaymentEntity(
				"order-2",
				"customer-1",
				PaymentProvider.PAYTR,
				BigDecimal.valueOf(150),
				"ext-2",
				"https://checkout.example.com/ext-2",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		);
		paymentRepository.saveAndFlush(payment);

		assertThat(paymentRepository.findByExternalPaymentIdAndProvider("ext-2", PaymentProvider.PAYTR))
				.isPresent()
				.get()
				.satisfies(foundPayment -> {
					assertThat(foundPayment.getOrderId()).isEqualTo("order-2");
					assertThat(foundPayment.getProvider()).isEqualTo(PaymentProvider.PAYTR);
				});
	}

	@Test
	void findByCustomerIdOrderByCreatedAtDescReturnsNewestPaymentsFirst() throws Exception {
		PaymentEntity firstPayment = new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"ext-1",
				"https://checkout.example.com/ext-1",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		);
		paymentRepository.saveAndFlush(firstPayment);

		Thread.sleep(5L);

		PaymentEntity secondPayment = new PaymentEntity(
				"order-2",
				"customer-1",
				PaymentProvider.PAYTR,
				BigDecimal.valueOf(150),
				"ext-2",
				"https://checkout.example.com/ext-2",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		);
		paymentRepository.saveAndFlush(secondPayment);

		List<PaymentEntity> result = paymentRepository.findByCustomerIdOrderByCreatedAtDesc(
				"customer-1",
				PageRequest.of(0, 10)
		).getContent();

		assertThat(result)
				.extracting(PaymentEntity::getOrderId)
				.containsExactly("order-2", "order-1");
	}
}
