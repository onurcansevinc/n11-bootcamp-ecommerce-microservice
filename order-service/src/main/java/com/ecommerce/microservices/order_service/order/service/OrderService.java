package com.ecommerce.microservices.order_service.order.service;

import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import com.ecommerce.microservices.order_service.cart.client.OrderCartClient;
import com.ecommerce.microservices.order_service.cart.dto.CartItemSummary;
import com.ecommerce.microservices.order_service.cart.dto.CartSummary;
import com.ecommerce.microservices.order_service.inventory.client.OrderInventoryClient;
import com.ecommerce.microservices.order_service.inventory.dto.InventoryReservationSummary;
import com.ecommerce.microservices.order_service.order.dto.CreateOrderRequest;
import com.ecommerce.microservices.order_service.order.dto.OrderResponse;
import com.ecommerce.microservices.order_service.order.entity.OrderEntity;
import com.ecommerce.microservices.order_service.order.entity.OrderItemEntity;
import com.ecommerce.microservices.order_service.order.exception.EmptyCartForOrderException;
import com.ecommerce.microservices.order_service.order.exception.InvalidOrderStateException;
import com.ecommerce.microservices.order_service.order.exception.OrderAccessDeniedException;
import com.ecommerce.microservices.order_service.order.exception.OrderNotFoundException;
import com.ecommerce.microservices.order_service.order.repository.OrderRepository;
import com.ecommerce.microservices.order_service.payment.processed.entity.ProcessedPaymentEventEntity;
import com.ecommerce.microservices.order_service.payment.processed.repository.ProcessedPaymentEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderCartClient orderCartClient;
	private final OrderInventoryClient orderInventoryClient;
	private final ProcessedPaymentEventRepository processedPaymentEventRepository;

	public OrderService(
			OrderRepository orderRepository,
			OrderCartClient orderCartClient,
			OrderInventoryClient orderInventoryClient,
			ProcessedPaymentEventRepository processedPaymentEventRepository
	) {
		this.orderRepository = orderRepository;
		this.orderCartClient = orderCartClient;
		this.orderInventoryClient = orderInventoryClient;
		this.processedPaymentEventRepository = processedPaymentEventRepository;
	}

	@Transactional
	public OrderResponse createOrder(String customerId, String bearerToken, CreateOrderRequest request) {
		CartSummary cart = orderCartClient.getRequiredCart(request.cartId(), bearerToken);

		if (cart.items() == null || cart.items().isEmpty()) {
			throw new EmptyCartForOrderException(request.cartId());
		}

		List<InventoryReservationSummary> reservations = new ArrayList<>();

		try {
			for (CartItemSummary item : cart.items()) {
				reservations.add(
						orderInventoryClient.createReservation(item.productId(), item.quantity())
				);
			}

			OrderEntity order = new OrderEntity(customerId, cart.id(), cart.totalAmount());
			for (int index = 0; index < cart.items().size(); index++) {
				CartItemSummary item = cart.items().get(index);
				InventoryReservationSummary reservation = reservations.get(index);

				order.addItem(new OrderItemEntity(
						item.productId(),
						item.productName(),
						item.unitPrice(),
						item.quantity(),
						reservation.reservationCode()
				));
			}

			return OrderResponse.from(orderRepository.save(order));
		} catch (RuntimeException exception) {
			releaseReservationsQuietly(reservations, bearerToken);
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrderById(String orderId, String customerId) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));

		if (!order.getCustomerId().equals(customerId)) {
			throw new OrderAccessDeniedException(orderId);
		}

		return OrderResponse.from(order);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> getOrders(String customerId, int page, int size) {
		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(
						customerId,
						PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
				)
				.map(OrderResponse::from);
	}

	@Transactional
	public void markPaymentSucceeded(String orderId) {
		OrderEntity order = getRequiredOrder(orderId);
		assertPendingPayment(order);

		for (OrderItemEntity item : order.getItemsOrderedById()) {
			orderInventoryClient.confirmReservation(item.getReservationCode());
		}

		order.markPaid();
	}

	@Transactional
	public void markPaymentFailed(String orderId) {
		OrderEntity order = getRequiredOrder(orderId);
		assertPendingPayment(order);

		for (OrderItemEntity item : order.getItemsOrderedById()) {
			orderInventoryClient.releaseReservation(item.getReservationCode());
		}

		order.markPaymentFailed();
	}

	@Transactional
	public void handlePaymentSucceededEvent(String eventId, String orderId) {
		if (processedPaymentEventRepository.existsById(eventId)) {
			return;
		}

		OrderEntity order = getRequiredOrder(orderId);
		if (order.getStatus() == com.ecommerce.microservices.order_service.order.entity.OrderStatus.PAID) {
			recordProcessedEvent(eventId, PaymentEventTypes.SUCCEEDED, orderId);
			return;
		}

		assertPendingPayment(order);
		for (OrderItemEntity item : order.getItemsOrderedById()) {
			orderInventoryClient.confirmReservation(item.getReservationCode());
		}

		order.markPaid();
		recordProcessedEvent(eventId, PaymentEventTypes.SUCCEEDED, orderId);
	}

	@Transactional
	public void handlePaymentFailedEvent(String eventId, String orderId) {
		if (processedPaymentEventRepository.existsById(eventId)) {
			return;
		}

		OrderEntity order = getRequiredOrder(orderId);
		if (order.getStatus() == com.ecommerce.microservices.order_service.order.entity.OrderStatus.PAYMENT_FAILED) {
			recordProcessedEvent(eventId, PaymentEventTypes.FAILED, orderId);
			return;
		}

		assertPendingPayment(order);
		for (OrderItemEntity item : order.getItemsOrderedById()) {
			orderInventoryClient.releaseReservation(item.getReservationCode());
		}

		order.markPaymentFailed();
		recordProcessedEvent(eventId, PaymentEventTypes.FAILED, orderId);
	}

	private void releaseReservationsQuietly(List<InventoryReservationSummary> reservations, String bearerToken) {
		for (InventoryReservationSummary reservation : reservations) {
			try {
				orderInventoryClient.releaseReservation(reservation.reservationCode());
			} catch (RuntimeException ignored) {
				// Best effort compensation. RabbitMQ/outbox phase will make this explicit.
			}
		}
	}

	private OrderEntity getRequiredOrder(String orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	private void assertPendingPayment(OrderEntity order) {
		if (order.getStatus() != com.ecommerce.microservices.order_service.order.entity.OrderStatus.PENDING_PAYMENT) {
			throw new InvalidOrderStateException(
					order.getId(),
					"PENDING_PAYMENT",
					order.getStatus().name()
			);
		}
	}

	private void recordProcessedEvent(String eventId, String eventType, String orderId) {
		processedPaymentEventRepository.save(new ProcessedPaymentEventEntity(eventId, eventType, orderId));
	}

}
