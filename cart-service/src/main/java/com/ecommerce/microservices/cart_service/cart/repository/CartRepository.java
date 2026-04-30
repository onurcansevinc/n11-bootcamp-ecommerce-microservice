package com.ecommerce.microservices.cart_service.cart.repository;

import com.ecommerce.microservices.cart_service.cart.entity.Cart;
import com.ecommerce.microservices.cart_service.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

	@Override
	@EntityGraph(attributePaths = "items")
	Optional<Cart> findById(String cartId);

	@EntityGraph(attributePaths = "items")
	Optional<Cart> findByCustomerIdAndStatus(String customerId, CartStatus status);
}
