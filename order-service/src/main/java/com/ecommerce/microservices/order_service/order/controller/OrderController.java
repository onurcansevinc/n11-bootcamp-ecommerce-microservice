package com.ecommerce.microservices.order_service.order.controller;

import com.ecommerce.microservices.common.web.response.ApiResponse;
import com.ecommerce.microservices.common.web.response.ResponseMeta;
import com.ecommerce.microservices.order_service.order.dto.CreateOrderRequest;
import com.ecommerce.microservices.order_service.order.dto.OrderResponse;
import com.ecommerce.microservices.order_service.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Orders", description = "Order creation and order query endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@Operation(summary = "Create order from cart")
	public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
			@Valid @RequestBody CreateOrderRequest request,
			JwtAuthenticationToken authentication
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"Order created successfully",
						orderService.createOrder(
								currentCustomerId(authentication),
								bearerToken(authentication),
								request
						)
				));
	}

	@GetMapping("/{orderId}")
	@Operation(summary = "Get order by id")
	public ApiResponse<OrderResponse> getOrderById(
			@PathVariable String orderId,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Order fetched successfully",
				orderService.getOrderById(orderId, currentCustomerId(authentication))
		);
	}

	@GetMapping
	@Operation(summary = "List current user's orders")
	public ApiResponse<List<OrderResponse>> getOrders(
			@RequestParam(defaultValue = "0") @Min(0) Integer page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
			JwtAuthenticationToken authentication
	) {
		Page<OrderResponse> orders = orderService.getOrders(currentCustomerId(authentication), page, size);
		return ApiResponse.success(
				"Orders fetched successfully",
				orders.getContent(),
				ResponseMeta.from(orders)
		);
	}

	private String currentCustomerId(JwtAuthenticationToken authentication) {
		return authentication.getToken().getSubject();
	}

	private String bearerToken(JwtAuthenticationToken authentication) {
		return authentication.getToken().getTokenValue();
	}

}
