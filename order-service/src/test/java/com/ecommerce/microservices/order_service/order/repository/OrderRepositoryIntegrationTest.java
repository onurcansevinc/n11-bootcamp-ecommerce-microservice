package com.ecommerce.microservices.order_service.order.repository;

import com.ecommerce.microservices.order_service.order.entity.OrderEntity;
import com.ecommerce.microservices.order_service.order.entity.OrderItemEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
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
		"spring.flyway.default-schema=ordering",
		"spring.flyway.schemas=ordering",
		"spring.jpa.properties.hibernate.default_schema=ordering"
})
class OrderRepositoryIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("order-service-test")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	private OrderRepository orderRepository;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void findByCustomerIdOrderByCreatedAtDescReturnsMostRecentOrdersFirst() throws Exception {
		OrderEntity firstOrder = new OrderEntity("customer-1", "cart-1", BigDecimal.valueOf(100));
		firstOrder.addItem(new OrderItemEntity(101L, "Keyboard", BigDecimal.valueOf(100), 1, "res-1"));
		orderRepository.saveAndFlush(firstOrder);

		Thread.sleep(5L);

		OrderEntity secondOrder = new OrderEntity("customer-1", "cart-2", BigDecimal.valueOf(250));
		secondOrder.addItem(new OrderItemEntity(102L, "Mouse", BigDecimal.valueOf(50), 1, "res-2"));
		secondOrder.addItem(new OrderItemEntity(103L, "Monitor", BigDecimal.valueOf(200), 1, "res-3"));
		orderRepository.saveAndFlush(secondOrder);

		Page<OrderEntity> result = orderRepository.findByCustomerIdOrderByCreatedAtDesc(
				"customer-1",
				PageRequest.of(0, 10)
		);

		assertThat(result.getContent())
				.extracting(OrderEntity::getSourceCartId)
				.containsExactly("cart-2", "cart-1");
		assertThat(result.getContent().get(0).getItemsOrderedById())
				.extracting(OrderItemEntity::getProductId)
				.containsExactly(102L, 103L);
	}

	@Test
	void findByCustomerIdOrderByCreatedAtDescFiltersByCustomer() {
		OrderEntity customerOneOrder = new OrderEntity("customer-1", "cart-1", BigDecimal.valueOf(100));
		customerOneOrder.addItem(new OrderItemEntity(101L, "Keyboard", BigDecimal.valueOf(100), 1, "res-1"));
		orderRepository.saveAndFlush(customerOneOrder);

		OrderEntity customerTwoOrder = new OrderEntity("customer-2", "cart-2", BigDecimal.valueOf(50));
		customerTwoOrder.addItem(new OrderItemEntity(102L, "Mouse", BigDecimal.valueOf(50), 1, "res-2"));
		orderRepository.saveAndFlush(customerTwoOrder);

		List<OrderEntity> result = orderRepository.findByCustomerIdOrderByCreatedAtDesc(
				"customer-1",
				PageRequest.of(0, 10)
		).getContent();

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getCustomerId()).isEqualTo("customer-1");
		assertThat(result.getFirst().getSourceCartId()).isEqualTo("cart-1");
	}
}
