package com.ecommerce.microservices.api_gateway.common.security;

import com.ecommerce.microservices.api_gateway.common.error.GatewayErrorResponse;
import com.ecommerce.microservices.api_gateway.common.filter.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public GatewayAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
		return writeResponse(
				exchange,
				HttpStatus.UNAUTHORIZED,
				"Authentication is required to access this resource"
		);
	}

	private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String message) {
		ServerHttpResponse response = exchange.getResponse();
		String correlationId = resolveCorrelationId(exchange);
		GatewayErrorResponse errorResponse = new GatewayErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				exchange.getRequest().getPath().value(),
				correlationId
		);

		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		response.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);

		return response.writeWith(Mono.just(response.bufferFactory().wrap(serialize(errorResponse))));
	}

	private String resolveCorrelationId(ServerWebExchange exchange) {
		Object correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
		if (correlationId instanceof String value && !value.isBlank()) {
			return value;
		}

		String requestCorrelationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
		if (requestCorrelationId != null && !requestCorrelationId.isBlank()) {
			return requestCorrelationId.trim();
		}

		return UUID.randomUUID().toString();
	}

	private byte[] serialize(GatewayErrorResponse errorResponse) {
		try {
			return objectMapper.writeValueAsBytes(errorResponse);
		} catch (JsonProcessingException ignored) {
			String fallback = """
					{"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","correlationId":"%s"}
					""".formatted(
					errorResponse.timestamp(),
					errorResponse.status(),
					errorResponse.error(),
					errorResponse.message(),
					errorResponse.path(),
					errorResponse.correlationId()
			);
			return fallback.getBytes(StandardCharsets.UTF_8);
		}
	}

}
