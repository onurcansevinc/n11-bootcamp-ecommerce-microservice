package com.ecommerce.microservices.order_service.order.controller;

import com.ecommerce.microservices.common.web.security.CurrentCustomerIdResolver;
import com.ecommerce.microservices.order_service.common.exception.GlobalExceptionHandler;
import com.ecommerce.microservices.order_service.order.dto.CreateOrderRequest;
import com.ecommerce.microservices.order_service.order.dto.OrderItemResponse;
import com.ecommerce.microservices.order_service.order.dto.OrderResponse;
import com.ecommerce.microservices.order_service.order.entity.OrderStatus;
import com.ecommerce.microservices.order_service.order.exception.EmptyCartForOrderException;
import com.ecommerce.microservices.order_service.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = OrderController.class,
		excludeAutoConfiguration = {
				SecurityAutoConfiguration.class,
				OAuth2ResourceServerAutoConfiguration.class
		},
		properties = {
				"spring.cloud.config.enabled=false",
				"spring.cloud.config.import-check.enabled=false",
				"spring.cloud.discovery.enabled=false",
				"eureka.client.enabled=false"
		}
)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private OrderService orderService;

	@MockBean
	private CurrentCustomerIdResolver currentCustomerIdResolver;

	@Test
	void createOrderReturnsCreatedResponseAndDelegatesToService() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		OrderResponse orderResponse = new OrderResponse(
				"order-1",
				"customer-1",
				"cart-1",
				OrderStatus.PENDING_PAYMENT,
				List.of(new OrderItemResponse(
						1L,
						101L,
						"Keyboard",
						BigDecimal.valueOf(100),
						2,
						BigDecimal.valueOf(200),
						"res-1",
						LocalDateTime.now(),
						LocalDateTime.now()
				)),
				BigDecimal.valueOf(200),
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(orderService.createOrder(eq("customer-1"), eq("user-token"), any(CreateOrderRequest.class)))
				.thenReturn(orderResponse);

		mockMvc.perform(post("/api/v1/orders")
						.principal(authentication)
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOrderRequest("cart-1"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Order created successfully"))
				.andExpect(jsonPath("$.data.id").value("order-1"))
				.andExpect(jsonPath("$.data.customerId").value("customer-1"))
				.andExpect(jsonPath("$.data.sourceCartId").value("cart-1"))
				.andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.data.totalAmount").value(200));

		verify(orderService).createOrder("customer-1", "user-token", new CreateOrderRequest("cart-1"));
	}

	@Test
	void createOrderReturnsBadRequestForInvalidBody() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");

		mockMvc.perform(post("/api/v1/orders")
						.principal(authentication)
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "cartId": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.detail").value("Request body validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("cartId"));

		verify(orderService, never()).createOrder(any(), any(), any());
	}

	@Test
	void createOrderMapsBusinessConflictToProblemDetail() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(orderService.createOrder(eq("customer-1"), eq("user-token"), any(CreateOrderRequest.class)))
				.thenThrow(new EmptyCartForOrderException("cart-1"));

		mockMvc.perform(post("/api/v1/orders")
						.principal(authentication)
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateOrderRequest("cart-1"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Empty cart"))
				.andExpect(jsonPath("$.detail").value("Cart cart-1 has no items to order"));
	}

	@Test
	void getOrdersReturnsPaginatedResponseMeta() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		OrderResponse orderResponse = new OrderResponse(
				"order-1",
				"customer-1",
				"cart-1",
				OrderStatus.PAID,
				List.of(),
				BigDecimal.valueOf(200),
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(orderService.getOrders("customer-1", 0, 10))
				.thenReturn(new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 10), 1));

		mockMvc.perform(get("/api/v1/orders")
						.principal(authentication)
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Orders fetched successfully"))
				.andExpect(jsonPath("$.data[0].id").value("order-1"))
				.andExpect(jsonPath("$.meta.page").value(0))
				.andExpect(jsonPath("$.meta.size").value(10))
				.andExpect(jsonPath("$.meta.totalElements").value(1))
				.andExpect(jsonPath("$.meta.totalPages").value(1))
				.andExpect(jsonPath("$.meta.hasNext").value(false))
				.andExpect(jsonPath("$.meta.hasPrevious").value(false));
	}

	private JwtAuthenticationToken jwtAuthenticationToken(String tokenValue) {
		Jwt jwt = Jwt.withTokenValue(tokenValue)
				.header("alg", "none")
				.subject("customer-1")
				.claim("preferred_username", "customer-user")
				.build();
		return new JwtAuthenticationToken(jwt);
	}
}
