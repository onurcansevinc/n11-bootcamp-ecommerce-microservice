package com.ecommerce.microservices.payment_service.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

	public PaymentNotFoundException(String paymentId) {
		super("Payment not found with id " + paymentId);
	}

}
