package com.ecommerce.microservices.payment_service.payment.exception;

public class DuplicatePendingPaymentException extends RuntimeException {

	public DuplicatePendingPaymentException(String orderId) {
		super("Order " + orderId + " already has an active or successful payment");
	}

}
