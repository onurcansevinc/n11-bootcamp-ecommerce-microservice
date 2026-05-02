package com.ecommerce.microservices.payment_service.payment.dto;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
		String id,
		String orderId,
		String customerId,
		PaymentProvider provider,
		PaymentStatus status,
		BigDecimal amount,
		String externalPaymentId,
		String checkoutUrl,
		String failureReason,
		LocalDateTime completedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static PaymentResponse from(PaymentEntity payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getOrderId(),
				payment.getCustomerId(),
				payment.getProvider(),
				payment.getStatus(),
				payment.getAmount(),
				payment.getExternalPaymentId(),
				payment.getCheckoutUrl(),
				payment.getFailureReason(),
				payment.getCompletedAt(),
				payment.getCreatedAt(),
				payment.getUpdatedAt()
		);
	}
}
