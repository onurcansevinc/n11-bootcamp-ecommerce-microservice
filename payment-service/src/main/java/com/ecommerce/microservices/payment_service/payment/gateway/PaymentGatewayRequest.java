package com.ecommerce.microservices.payment_service.payment.gateway;

import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentCheckoutRequest;

public record PaymentGatewayRequest(
		OrderSummary order,
		String customerId,
		PaymentCheckoutRequest checkout,
		String clientIp
) {
}
