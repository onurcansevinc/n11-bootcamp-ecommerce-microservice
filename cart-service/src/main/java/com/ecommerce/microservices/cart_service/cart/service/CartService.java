package com.ecommerce.microservices.cart_service.cart.service;

import com.ecommerce.microservices.cart_service.cart.dto.CartResponse;
import com.ecommerce.microservices.cart_service.cart.dto.CreateCartItemRequest;
import com.ecommerce.microservices.cart_service.cart.dto.UpdateCartItemRequest;
import com.ecommerce.microservices.cart_service.cart.entity.Cart;
import com.ecommerce.microservices.cart_service.cart.entity.CartItem;
import com.ecommerce.microservices.cart_service.cart.entity.CartStatus;
import com.ecommerce.microservices.cart_service.cart.exception.CartAccessDeniedException;
import com.ecommerce.microservices.cart_service.cart.exception.CartItemNotFoundException;
import com.ecommerce.microservices.cart_service.cart.exception.CartNotFoundException;
import com.ecommerce.microservices.cart_service.cart.exception.InvalidCartStateException;
import com.ecommerce.microservices.cart_service.cart.repository.CartRepository;
import com.ecommerce.microservices.cart_service.catalog.client.ProductCatalogClient;
import com.ecommerce.microservices.cart_service.catalog.dto.ProductSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

	private final CartRepository cartRepository;
	private final ProductCatalogClient productCatalogClient;

	public CartService(CartRepository cartRepository, ProductCatalogClient productCatalogClient) {
		this.cartRepository = cartRepository;
		this.productCatalogClient = productCatalogClient;
	}

	@Transactional
	public CartResponse createCart(String customerId) {
		Cart cart = cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
				.orElseGet(() -> cartRepository.save(new Cart(customerId)));

		return CartResponse.from(cart);
	}

	@Transactional(readOnly = true)
	public CartResponse getCartById(String cartId, String customerId) {
		return CartResponse.from(getOwnedActiveCart(cartId, customerId));
	}

	@Transactional
	public CartResponse addItem(String cartId, String customerId, CreateCartItemRequest request) {
		Cart cart = getOwnedActiveCart(cartId, customerId);
		ProductSummary product = productCatalogClient.getRequiredActiveProduct(request.productId());

		cart.addOrIncrementItem(
				product.id(),
				product.name(),
				product.price(),
				request.quantity()
		);

		return CartResponse.from(cartRepository.save(cart));
	}

	@Transactional
	public CartResponse updateItemQuantity(
			String cartId,
			Long itemId,
			String customerId,
			UpdateCartItemRequest request
	) {
		Cart cart = getOwnedActiveCart(cartId, customerId);
		CartItem cartItem = cart.findItemById(itemId)
				.orElseThrow(() -> new CartItemNotFoundException(itemId, cartId));

		cartItem.changeQuantity(request.quantity());
		return CartResponse.from(cartRepository.save(cart));
	}

	@Transactional
	public void deleteItem(String cartId, Long itemId, String customerId) {
		Cart cart = getOwnedActiveCart(cartId, customerId);
		CartItem cartItem = cart.findItemById(itemId)
				.orElseThrow(() -> new CartItemNotFoundException(itemId, cartId));

		cart.removeItem(cartItem.getId());
		cartRepository.save(cart);
	}

	private Cart getOwnedActiveCart(String cartId, String customerId) {
		Cart cart = cartRepository.findById(cartId)
				.orElseThrow(() -> new CartNotFoundException(cartId));

		if (!cart.getCustomerId().equals(customerId)) {
			throw new CartAccessDeniedException(cartId);
		}

		if (cart.getStatus() != CartStatus.ACTIVE) {
			throw new InvalidCartStateException("Cart " + cartId + " is not active");
		}

		return cart;
	}

}
