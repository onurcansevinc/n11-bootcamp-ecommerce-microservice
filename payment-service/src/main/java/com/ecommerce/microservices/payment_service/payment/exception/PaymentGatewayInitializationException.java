package com.ecommerce.microservices.payment_service.payment.exception;

public class PaymentGatewayInitializationException extends RuntimeException {

	public PaymentGatewayInitializationException(String provider, String message) {
		super("Payment gateway " + provider + " initialization failed: " + message);
	}

}
