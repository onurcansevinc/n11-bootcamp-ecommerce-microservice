package com.ecommerce.microservices.payment_service.payment.exception;

public class InvalidPaymentStateException extends RuntimeException {

	public InvalidPaymentStateException(String paymentId, String expectedStatus, String actualStatus) {
		super("Payment " + paymentId + " must be in status " + expectedStatus + " but is " + actualStatus);
	}

}
