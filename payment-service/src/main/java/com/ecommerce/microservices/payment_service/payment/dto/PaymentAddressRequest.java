package com.ecommerce.microservices.payment_service.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentAddressRequest(
		@NotBlank String contactName,
		@NotBlank String address,
		@NotBlank String city,
		@NotBlank String country,
		@NotBlank String zipCode
) {
}
