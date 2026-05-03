package com.ecommerce.microservices.cart_service.cart.controller;

import com.ecommerce.microservices.cart_service.cart.dto.CartItemResponse;
import com.ecommerce.microservices.cart_service.cart.dto.CartResponse;
import com.ecommerce.microservices.cart_service.cart.dto.CreateCartItemRequest;
import com.ecommerce.microservices.cart_service.cart.dto.UpdateCartItemRequest;
import com.ecommerce.microservices.cart_service.cart.entity.CartStatus;
import com.ecommerce.microservices.cart_service.cart.exception.CartAccessDeniedException;
import com.ecommerce.microservices.cart_service.cart.service.CartService;
import com.ecommerce.microservices.cart_service.common.exception.GlobalExceptionHandler;
import com.ecommerce.microservices.common.web.security.CurrentCustomerIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CartService cartService;

	@MockBean
	private CurrentCustomerIdResolver currentCustomerIdResolver;

	@Test
	void createCartReturnsCreatedResponse() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken();
		CartResponse cartResponse = sampleCartResponse();

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(cartService.createCart("customer-1")).thenReturn(cartResponse);

		mockMvc.perform(post("/api/v1/carts").principal(authentication))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Cart created successfully"))
				.andExpect(jsonPath("$.data.id").value(cartResponse.id()))
				.andExpect(jsonPath("$.data.customerId").value("customer-1"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.totalAmount").value(249.9));

		verify(cartService).createCart("customer-1");
	}

	@Test
	void getCartByIdReturnsCartResponse() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken();
		CartResponse cartResponse = sampleCartResponse();

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(cartService.getCartById("cart-1", "customer-1")).thenReturn(cartResponse);

		mockMvc.perform(get("/api/v1/carts/cart-1").principal(authentication))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Cart fetched successfully"))
				.andExpect(jsonPath("$.data.id").value("cart-1"))
				.andExpect(jsonPath("$.data.items[0].productName").value("Keyboard"));
	}

	@Test
	void addItemReturnsUpdatedCartResponse() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken();
		CreateCartItemRequest request = new CreateCartItemRequest(1L, 2);
		CartResponse cartResponse = sampleCartResponse();

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(cartService.addItem("cart-1", "customer-1", request)).thenReturn(cartResponse);

		mockMvc.perform(post("/api/v1/carts/cart-1/items")
						.principal(authentication)
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Cart item added successfully"))
				.andExpect(jsonPath("$.data.id").value("cart-1"))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2));

		verify(cartService).addItem("cart-1", "customer-1", request);
	}

	@Test
	void updateItemQuantityReturnsBadRequestForInvalidBody() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken();
		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");

		mockMvc.perform(patch("/api/v1/carts/cart-1/items/10")
						.principal(authentication)
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "quantity": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.detail").value("Request body validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("quantity"));

		verify(cartService, never()).updateItemQuantity(any(), any(), any(), any());
	}

	@Test
	void getCartByIdMapsAccessDeniedToProblemDetail() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken();

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(cartService.getCartById("cart-1", "customer-1"))
				.thenThrow(new CartAccessDeniedException("cart-1"));

		mockMvc.perform(get("/api/v1/carts/cart-1").principal(authentication))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.title").value("Cart access denied"))
				.andExpect(jsonPath("$.detail").value("You do not have access to cart cart-1"));
	}

	@Test
	void deleteItemReturnsSuccessResponse() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken();
		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");

		mockMvc.perform(delete("/api/v1/carts/cart-1/items/10").principal(authentication))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Cart item deleted successfully"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(cartService).deleteItem("cart-1", 10L, "customer-1");
	}

	private CartResponse sampleCartResponse() {
		return new CartResponse(
				"cart-1",
				"customer-1",
				CartStatus.ACTIVE,
				List.of(new CartItemResponse(
						10L,
						1L,
						"Keyboard",
						BigDecimal.valueOf(124.95),
						2,
						BigDecimal.valueOf(249.90),
						LocalDateTime.now(),
						LocalDateTime.now()
				)),
				BigDecimal.valueOf(249.90),
				LocalDateTime.now(),
				LocalDateTime.now()
		);
	}

	private JwtAuthenticationToken jwtAuthenticationToken() {
		Jwt jwt = Jwt.withTokenValue("user-token")
				.header("alg", "none")
				.subject("customer-1")
				.claim("preferred_username", "customer-user")
				.build();
		return new JwtAuthenticationToken(jwt);
	}

}
