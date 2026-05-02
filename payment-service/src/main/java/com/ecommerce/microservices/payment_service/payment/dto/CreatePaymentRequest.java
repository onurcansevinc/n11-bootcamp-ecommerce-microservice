package com.ecommerce.microservices.payment_service.payment.dto;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
		@NotBlank String orderId,
		@NotNull PaymentProvider provider,
		@Valid @NotNull PaymentCheckoutRequest checkout
) {
}
