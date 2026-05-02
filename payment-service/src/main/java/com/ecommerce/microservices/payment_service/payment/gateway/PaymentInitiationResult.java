package com.ecommerce.microservices.payment_service.payment.gateway;

public record PaymentInitiationResult(
		String externalPaymentId,
		String checkoutUrl
) {
}
