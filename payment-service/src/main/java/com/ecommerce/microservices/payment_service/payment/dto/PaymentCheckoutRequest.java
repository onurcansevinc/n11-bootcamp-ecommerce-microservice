package com.ecommerce.microservices.payment_service.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCheckoutRequest(
		@NotBlank String locale,
		@Valid @NotNull PaymentBuyerRequest buyer,
		@Valid @NotNull PaymentAddressRequest billingAddress,
		@Valid @NotNull PaymentAddressRequest shippingAddress
) {
}
