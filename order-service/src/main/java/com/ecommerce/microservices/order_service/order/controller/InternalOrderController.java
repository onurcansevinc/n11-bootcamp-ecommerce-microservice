package com.ecommerce.microservices.order_service.order.controller;

import com.ecommerce.microservices.order_service.order.service.OrderService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

	private final OrderService orderService;

	public InternalOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/{orderId}/payment-success")
	public ResponseEntity<Void> markPaymentSucceeded(@PathVariable String orderId) {
		orderService.markPaymentSucceeded(orderId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{orderId}/payment-failure")
	public ResponseEntity<Void> markPaymentFailed(@PathVariable String orderId) {
		orderService.markPaymentFailed(orderId);
		return ResponseEntity.noContent().build();
	}

}
