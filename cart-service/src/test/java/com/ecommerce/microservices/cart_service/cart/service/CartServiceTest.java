package com.ecommerce.microservices.cart_service.cart.service;

import com.ecommerce.microservices.cart_service.cart.dto.CartResponse;
import com.ecommerce.microservices.cart_service.cart.entity.Cart;
import com.ecommerce.microservices.cart_service.cart.entity.CartStatus;
import com.ecommerce.microservices.cart_service.cart.repository.CartRepository;
import com.ecommerce.microservices.cart_service.catalog.client.ProductCatalogClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private ProductCatalogClient productCatalogClient;

	@InjectMocks
	private CartService cartService;

	@Test
	void createCartMergesDuplicateActiveCartsForSameCustomer() {
		Cart primaryCart = new Cart("customer-user");
		primaryCart.addOrIncrementItem(1L, "Keyboard", BigDecimal.valueOf(100), 2);

		Cart duplicateCart = new Cart("customer-user");
		duplicateCart.addOrIncrementItem(1L, "Keyboard", BigDecimal.valueOf(100), 3);
		duplicateCart.addOrIncrementItem(2L, "Mouse", BigDecimal.valueOf(50), 1);

		when(cartRepository.findAllByCustomerIdAndStatusOrderByUpdatedAtDesc(
				"customer-user",
				CartStatus.ACTIVE
		)).thenReturn(List.of(primaryCart, duplicateCart));
		when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CartResponse response = cartService.createCart("customer-user");

		assertThat(response.customerId()).isEqualTo("customer-user");
		assertThat(response.items()).hasSize(2);
		assertThat(response.items())
				.filteredOn(item -> item.productId().equals(1L))
				.singleElement()
				.satisfies(item -> assertThat(item.quantity()).isEqualTo(5));
		assertThat(response.items())
				.filteredOn(item -> item.productId().equals(2L))
				.singleElement()
				.satisfies(item -> assertThat(item.quantity()).isEqualTo(1));

		ArgumentCaptor<List<Cart>> deletedCartsCaptor = ArgumentCaptor.forClass(List.class);
		verify(cartRepository).deleteAll(deletedCartsCaptor.capture());
		assertThat(deletedCartsCaptor.getValue()).containsExactly(duplicateCart);
	}

	@Test
	void createCartCreatesNewCartWhenNoActiveCartExists() {
		when(cartRepository.findAllByCustomerIdAndStatusOrderByUpdatedAtDesc(
				"customer-user",
				CartStatus.ACTIVE
		)).thenReturn(List.of());
		when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CartResponse response = cartService.createCart("customer-user");

		assertThat(response.customerId()).isEqualTo("customer-user");
		assertThat(response.status()).isEqualTo(CartStatus.ACTIVE);
		verify(cartRepository, never()).deleteAll(any(List.class));
	}

}
