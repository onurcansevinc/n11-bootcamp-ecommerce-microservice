package com.ecommerce.microservices.payment_service.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PaymentBuyerRequest(
		@NotBlank String name,
		@NotBlank String surname,
		@Email @NotBlank String email,
		@NotBlank String gsmNumber,
		@NotBlank String identityNumber,
		String registrationAddress,
		String city,
		String country,
		String zipCode
) {
}
