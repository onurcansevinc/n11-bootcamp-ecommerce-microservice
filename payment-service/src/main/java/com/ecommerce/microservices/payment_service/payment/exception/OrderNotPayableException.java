package com.ecommerce.microservices.payment_service.payment.exception;

public class OrderNotPayableException extends RuntimeException {

	public OrderNotPayableException(String orderId, String status) {
		super("Order " + orderId + " cannot be paid because it is in status " + status);
	}

}
