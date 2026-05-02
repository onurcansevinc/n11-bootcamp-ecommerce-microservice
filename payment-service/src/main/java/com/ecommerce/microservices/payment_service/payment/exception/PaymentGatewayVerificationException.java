package com.ecommerce.microservices.payment_service.payment.exception;

public class PaymentGatewayVerificationException extends RuntimeException {

	public PaymentGatewayVerificationException(String provider, String message) {
		super("Payment gateway " + provider + " verification failed: " + message);
	}

}
