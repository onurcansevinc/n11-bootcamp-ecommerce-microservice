package com.ecommerce.microservices.payment_service.order.client;

import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;

public interface PaymentOrderClient {

	OrderSummary getRequiredOrder(String orderId, String bearerToken);

	void markPaymentSucceeded(String orderId);

	void markPaymentFailed(String orderId);

}
