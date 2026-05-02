package com.ecommerce.microservices.payment_service.payment.exception;

public class UnsupportedPaymentProviderException extends RuntimeException {

	public UnsupportedPaymentProviderException(String provider) {
		super("Payment provider is not supported: " + provider);
	}

}
