package com.ecommerce.microservices.cart_service.cart.controller;

import com.ecommerce.microservices.cart_service.cart.dto.CartResponse;
import com.ecommerce.microservices.cart_service.cart.dto.CreateCartItemRequest;
import com.ecommerce.microservices.cart_service.cart.dto.UpdateCartItemRequest;
import com.ecommerce.microservices.cart_service.cart.service.CartService;
import com.ecommerce.microservices.common.web.response.ApiResponse;
import com.ecommerce.microservices.common.web.security.CurrentCustomerIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Carts", description = "Shopping cart endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

	private final CartService cartService;
	private final CurrentCustomerIdResolver currentCustomerIdResolver;

	public CartController(CartService cartService, CurrentCustomerIdResolver currentCustomerIdResolver) {
		this.cartService = cartService;
		this.currentCustomerIdResolver = currentCustomerIdResolver;
	}

	@PostMapping
	@Operation(summary = "Create or return active cart for current user")
	public ResponseEntity<ApiResponse<CartResponse>> createCart(JwtAuthenticationToken authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"Cart created successfully",
						cartService.createCart(currentCustomerId(authentication))
				));
	}

	@GetMapping("/{cartId}")
	@Operation(summary = "Get cart by id")
	public ApiResponse<CartResponse> getCartById(
			@PathVariable String cartId,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Cart fetched successfully",
				cartService.getCartById(cartId, currentCustomerId(authentication))
		);
	}

	@PostMapping("/{cartId}/items")
	@Operation(summary = "Add item to cart")
	public ApiResponse<CartResponse> addItem(
			@PathVariable String cartId,
			@Valid @RequestBody CreateCartItemRequest request,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Cart item added successfully",
				cartService.addItem(cartId, currentCustomerId(authentication), request)
		);
	}

	@PatchMapping("/{cartId}/items/{itemId}")
	@Operation(summary = "Update cart item quantity")
	public ApiResponse<CartResponse> updateItemQuantity(
			@PathVariable String cartId,
			@PathVariable Long itemId,
			@Valid @RequestBody UpdateCartItemRequest request,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Cart item updated successfully",
				cartService.updateItemQuantity(cartId, itemId, currentCustomerId(authentication), request)
		);
	}

	@DeleteMapping("/{cartId}/items/{itemId}")
	@Operation(summary = "Delete cart item")
	public ApiResponse<Void> deleteItem(
			@PathVariable String cartId,
			@PathVariable Long itemId,
			JwtAuthenticationToken authentication
	) {
		cartService.deleteItem(cartId, itemId, currentCustomerId(authentication));
		return ApiResponse.success("Cart item deleted successfully", null);
	}

	private String currentCustomerId(JwtAuthenticationToken authentication) {
		return currentCustomerIdResolver.resolve(authentication);
	}

}
