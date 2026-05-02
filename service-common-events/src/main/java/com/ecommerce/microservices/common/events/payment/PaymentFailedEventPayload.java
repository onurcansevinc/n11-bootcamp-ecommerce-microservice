package com.ecommerce.microservices.common.events.payment;

import java.math.BigDecimal;

public record PaymentFailedEventPayload(
		String paymentId,
		String orderId,
		String customerId,
		String provider,
		BigDecimal amount,
		String failureReason
) {
}
