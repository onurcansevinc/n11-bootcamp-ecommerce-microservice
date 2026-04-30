package com.ecommerce.microservices.order_service.cart.client;

import com.ecommerce.microservices.order_service.cart.dto.CartSummary;

public interface OrderCartClient {

	CartSummary getRequiredCart(String cartId, String bearerToken);

}
