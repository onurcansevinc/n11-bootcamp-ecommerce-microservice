package com.ecommerce.microservices.payment_service.payment.gateway;

public record PaymentVerificationResult(
		boolean successful,
		String failureReason
) {
	public static PaymentVerificationResult success() {
		return new PaymentVerificationResult(true, null);
	}

	public static PaymentVerificationResult failure(String failureReason) {
		return new PaymentVerificationResult(false, failureReason);
	}
}
