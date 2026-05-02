package com.ecommerce.microservices.payment_service.payment.exception;

public class OrderNotFoundForPaymentException extends RuntimeException {

	public OrderNotFoundForPaymentException(String orderId) {
		super("Order not found with id " + orderId);
	}

}
