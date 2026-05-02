package com.ecommerce.microservices.payment_service.payment.exception;

public class OrderServiceUnavailableForPaymentException extends RuntimeException {

	public OrderServiceUnavailableForPaymentException(String message, Throwable cause) {
		super(message, cause);
	}

}
