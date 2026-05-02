package com.ecommerce.microservices.payment_service.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment.iyzico")
public record IyzicoProperties(
		@NotBlank String apiKey,
		@NotBlank String secretKey,
		@NotBlank String baseUrl,
		@NotBlank String callbackUrl
) {
}
