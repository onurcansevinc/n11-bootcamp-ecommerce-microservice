package com.ecommerce.microservices.common.events.payment;

import java.math.BigDecimal;

public record PaymentSucceededEventPayload(
		String paymentId,
		String orderId,
		String customerId,
		String customerName,
		String customerSurname,
		String customerEmail,
		String customerGsmNumber,
		String provider,
		BigDecimal amount
) {
}
