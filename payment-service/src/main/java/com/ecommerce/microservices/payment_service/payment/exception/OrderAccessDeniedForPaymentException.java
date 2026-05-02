package com.ecommerce.microservices.payment_service.payment.exception;

public class OrderAccessDeniedForPaymentException extends RuntimeException {

	public OrderAccessDeniedForPaymentException(String orderId) {
		super("You are not allowed to access order " + orderId);
	}

}
