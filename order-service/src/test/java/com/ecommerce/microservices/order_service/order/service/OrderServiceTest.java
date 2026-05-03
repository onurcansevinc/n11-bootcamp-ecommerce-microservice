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
import com.ecommerce.microservices.order_service.order.repository.OrderRepository;
import com.ecommerce.microservices.order_service.payment.processed.entity.ProcessedPaymentEventEntity;
import com.ecommerce.microservices.order_service.payment.processed.repository.ProcessedPaymentEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderCartClient orderCartClient;

	@Mock
	private OrderInventoryClient orderInventoryClient;

	@Mock
	private ProcessedPaymentEventRepository processedPaymentEventRepository;

	@InjectMocks
	private OrderService orderService;

	@Test
	void createOrderCreatesOrderUsingCartItemsAndReservations() {
		CartSummary cart = new CartSummary(
				"cart-1",
				"customer-1",
				"ACTIVE",
				List.of(
						new CartItemSummary(1L, 101L, "Keyboard", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200)),
						new CartItemSummary(2L, 102L, "Mouse", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50))
				),
				BigDecimal.valueOf(250)
		);
		CreateOrderRequest request = new CreateOrderRequest("cart-1");

		when(orderCartClient.getRequiredCart("cart-1", "Bearer token")).thenReturn(cart);
		when(orderInventoryClient.createReservation(101L, 2))
				.thenReturn(new InventoryReservationSummary("res-1", 101L, 2, "RESERVED"));
		when(orderInventoryClient.createReservation(102L, 1))
				.thenReturn(new InventoryReservationSummary("res-2", 102L, 1, "RESERVED"));
		when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OrderResponse response = orderService.createOrder("customer-1", "Bearer token", request);

		assertThat(response.customerId()).isEqualTo("customer-1");
		assertThat(response.sourceCartId()).isEqualTo("cart-1");
		assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(response.totalAmount()).isEqualByComparingTo("250");
		assertThat(response.items()).hasSize(2);
		assertThat(response.items())
				.extracting(item -> item.reservationCode())
				.containsExactly("res-1", "res-2");

		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getItemsOrderedById())
				.extracting(OrderItemEntity::getProductId, OrderItemEntity::getQuantity, OrderItemEntity::getReservationCode)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(101L, 2, "res-1"),
						org.assertj.core.groups.Tuple.tuple(102L, 1, "res-2")
				);
	}

	@Test
	void createOrderReleasesReservationsWhenSavingOrderFails() {
		CartSummary cart = new CartSummary(
				"cart-1",
				"customer-1",
				"ACTIVE",
				List.of(
						new CartItemSummary(1L, 101L, "Keyboard", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200)),
						new CartItemSummary(2L, 102L, "Mouse", BigDecimal.valueOf(50), 1, BigDecimal.valueOf(50))
				),
				BigDecimal.valueOf(250)
		);

		when(orderCartClient.getRequiredCart("cart-1", "Bearer token")).thenReturn(cart);
		when(orderInventoryClient.createReservation(101L, 2))
				.thenReturn(new InventoryReservationSummary("res-1", 101L, 2, "RESERVED"));
		when(orderInventoryClient.createReservation(102L, 1))
				.thenReturn(new InventoryReservationSummary("res-2", 102L, 1, "RESERVED"));
		when(orderRepository.save(any(OrderEntity.class))).thenThrow(new IllegalStateException("db write failed"));

		assertThatThrownBy(() -> orderService.createOrder(
				"customer-1",
				"Bearer token",
				new CreateOrderRequest("cart-1")
		))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("db write failed");

		verify(orderInventoryClient).releaseReservation("res-1");
		verify(orderInventoryClient).releaseReservation("res-2");
	}

	@Test
	void handlePaymentSucceededEventConfirmsReservationsMarksOrderPaidAndRecordsEvent() {
		OrderEntity order = new OrderEntity("customer-1", "cart-1", BigDecimal.valueOf(250));
		order.addItem(new OrderItemEntity(101L, "Keyboard", BigDecimal.valueOf(100), 2, "res-1"));
		order.addItem(new OrderItemEntity(102L, "Mouse", BigDecimal.valueOf(50), 1, "res-2"));

		when(processedPaymentEventRepository.existsById("event-1")).thenReturn(false);
		when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
		when(processedPaymentEventRepository.save(any(ProcessedPaymentEventEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		orderService.handlePaymentSucceededEvent("event-1", order.getId());

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		verify(orderInventoryClient).confirmReservation("res-1");
		verify(orderInventoryClient).confirmReservation("res-2");

		ArgumentCaptor<ProcessedPaymentEventEntity> eventCaptor =
				ArgumentCaptor.forClass(ProcessedPaymentEventEntity.class);
		verify(processedPaymentEventRepository).save(eventCaptor.capture());
		assertThat(ReflectionTestUtils.getField(eventCaptor.getValue(), "eventId")).isEqualTo("event-1");
		assertThat(ReflectionTestUtils.getField(eventCaptor.getValue(), "orderId")).isEqualTo(order.getId());
	}

	@Test
	void handlePaymentSucceededEventSkipsAlreadyProcessedEvents() {
		when(processedPaymentEventRepository.existsById("event-1")).thenReturn(true);

		orderService.handlePaymentSucceededEvent("event-1", "order-1");

		verify(orderRepository, never()).findById(any());
		verify(orderInventoryClient, never()).confirmReservation(any());
		verify(processedPaymentEventRepository, never()).save(any());
	}
}
