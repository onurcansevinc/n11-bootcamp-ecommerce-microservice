package com.ecommerce.microservices.payment_service.payment.exception;

public class PaymentAccessDeniedException extends RuntimeException {

	public PaymentAccessDeniedException(String paymentId) {
		super("You are not allowed to access payment " + paymentId);
	}

}
