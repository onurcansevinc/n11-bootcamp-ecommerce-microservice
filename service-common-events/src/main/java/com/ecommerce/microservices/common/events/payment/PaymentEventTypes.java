package com.ecommerce.microservices.common.events.payment;

public final class PaymentEventTypes {

	public static final String VERSION = "v1";
	public static final String SUCCEEDED = "payment.succeeded.v1";
	public static final String FAILED = "payment.failed.v1";

	private PaymentEventTypes() {
	}
}
