package com.ecommerce.microservices.api_gateway.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		String correlationId
) {
}
