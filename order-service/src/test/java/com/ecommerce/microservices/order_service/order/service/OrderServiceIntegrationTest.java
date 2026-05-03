package com.ecommerce.microservices.order_service.order.service;

import com.ecommerce.microservices.order_service.cart.client.OrderCartClient;
import com.ecommerce.microservices.order_service.cart.dto.CartItemSummary;
import com.ecommerce.microservices.order_service.cart.dto.CartSummary;
import com.ecommerce.microservices.order_service.inventory.client.OrderInventoryClient;
import com.ecommerce.microservices.order_service.inventory.dto.InventoryReservationSummary;
import com.ecommerce.microservices.order_service.order.dto.CreateOrderRequest;
import com.ecommerce.microservices.order_service.order.dto.OrderResponse;
import com.ecommerce.microservices.order_service.order.entity.OrderEntity;
import com.ecommerce.microservices.order_service.order.entity.OrderItemEntity;
import com.ecommerce.microservices.order_service.order.entity.OrderStatus;
import com.ecommerce.microservices.order_service.order.exception.InventoryReservationFailedException;
import com.ecommerce.microservices.order_service.order.repository.OrderRepository;
import com.ecommerce.microservices.order_service.payment.processed.repository.ProcessedPaymentEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderService.class)
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
class OrderServiceIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("order-service-test")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProcessedPaymentEventRepository processedPaymentEventRepository;

	@MockBean
	private OrderCartClient orderCartClient;

	@MockBean
	private OrderInventoryClient orderInventoryClient;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void createOrderPersistsOrderItemsAndReservationCodes() {
		when(orderCartClient.getRequiredCart("cart-1", "Bearer token")).thenReturn(activeCart());
		when(orderInventoryClient.createReservation(101L, 2))
				.thenReturn(new InventoryReservationSummary("res-1", 101L, 2, "RESERVED"));
		when(orderInventoryClient.createReservation(102L, 1))
				.thenReturn(new InventoryReservationSummary("res-2", 102L, 1, "RESERVED"));

		OrderResponse response = orderService.createOrder(
				"customer-1",
				"Bearer token",
				new CreateOrderRequest("cart-1")
		);

		OrderEntity persistedOrder = orderRepository.findById(response.id()).orElseThrow();

		assertThat(persistedOrder.getCustomerId()).isEqualTo("customer-1");
		assertThat(persistedOrder.getSourceCartId()).isEqualTo("cart-1");
		assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(persistedOrder.getTotalAmount()).isEqualByComparingTo("250.00");
		assertThat(persistedOrder.getItemsOrderedById())
				.extracting(OrderItemEntity::getProductId, OrderItemEntity::getQuantity, OrderItemEntity::getReservationCode)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(101L, 2, "res-1"),
						org.assertj.core.groups.Tuple.tuple(102L, 1, "res-2")
				);
	}

	@Test
	void createOrderReleasesCompletedReservationsAndLeavesDatabaseEmptyWhenLaterReservationFails() {
		when(orderCartClient.getRequiredCart("cart-1", "Bearer token")).thenReturn(activeCart());
		when(orderInventoryClient.createReservation(101L, 2))
				.thenReturn(new InventoryReservationSummary("res-1", 101L, 2, "RESERVED"));
		when(orderInventoryClient.createReservation(102L, 1))
				.thenThrow(new InventoryReservationFailedException("Inventory item not found for product id 102"));

		assertThatThrownBy(() -> orderService.createOrder(
				"customer-1",
				"Bearer token",
				new CreateOrderRequest("cart-1")
		))
				.isInstanceOf(InventoryReservationFailedException.class)
				.hasMessage("Inventory item not found for product id 102");

		verify(orderInventoryClient).releaseReservation("res-1");
		assertThat(orderRepository.count()).isZero();
		assertThat(processedPaymentEventRepository.count()).isZero();
	}

	@Test
	void markPaymentSucceededConfirmsReservationsAndPersistsPaidStatus() {
		OrderEntity order = persistPendingOrder();

		orderService.markPaymentSucceeded(order.getId());

		OrderEntity updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
		verify(orderInventoryClient).confirmReservation("res-1");
		verify(orderInventoryClient).confirmReservation("res-2");
	}

	@Test
	void handlePaymentFailedEventReleasesReservationsMarksOrderAndRecordsEventOnlyOnce() {
		OrderEntity order = persistPendingOrder();

		orderService.handlePaymentFailedEvent("event-1", order.getId());
		orderService.handlePaymentFailedEvent("event-1", order.getId());

		OrderEntity updatedOrder = orderRepository.findById(order.getId()).orElseThrow();

		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
		assertThat(processedPaymentEventRepository.count()).isEqualTo(1);
		assertThat(processedPaymentEventRepository.existsById("event-1")).isTrue();
		Object processedEvent = processedPaymentEventRepository.findById("event-1").orElseThrow();
		assertThat(ReflectionTestUtils.getField(processedEvent, "orderId")).isEqualTo(order.getId());
		verify(orderInventoryClient, times(1)).releaseReservation("res-1");
		verify(orderInventoryClient, times(1)).releaseReservation("res-2");
	}

	private CartSummary activeCart() {
		return new CartSummary(
				"cart-1",
				"customer-1",
				"ACTIVE",
				List.of(
						new CartItemSummary(1L, 101L, "Keyboard", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200)),
						new CartItemSummary(2L, 102L, "Mouse", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50))
				),
				BigDecimal.valueOf(250)
		);
	}

	private OrderEntity persistPendingOrder() {
		OrderEntity order = new OrderEntity("customer-1", "cart-1", BigDecimal.valueOf(250));
		order.addItem(new OrderItemEntity(101L, "Keyboard", BigDecimal.valueOf(100), 2, "res-1"));
		order.addItem(new OrderItemEntity(102L, "Mouse", BigDecimal.valueOf(50), 1, "res-2"));
		return orderRepository.saveAndFlush(order);
	}

}
