package com.ecommerce.microservices.api_gateway.common.error;

import com.ecommerce.microservices.api_gateway.common.filter.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

	private final ObjectMapper objectMapper;

	public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) {
			return Mono.error(ex);
		}

		HttpStatusCode statusCode = resolveStatus(ex);
		String correlationId = resolveCorrelationId(exchange);
		GatewayErrorResponse errorResponse = new GatewayErrorResponse(
				Instant.now(),
				statusCode.value(),
				resolveReasonPhrase(statusCode),
				resolveMessage(ex),
				exchange.getRequest().getPath().value(),
				correlationId
		);

		response.setStatusCode(statusCode);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		if (correlationId != null && !correlationId.isBlank()) {
			response.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
		}

		byte[] body = serialize(errorResponse);
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
	}

	private HttpStatusCode resolveStatus(Throwable ex) {
		if (ex instanceof ResponseStatusException responseStatusException) {
			return responseStatusException.getStatusCode();
		}

		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	private String resolveReasonPhrase(HttpStatusCode statusCode) {
		HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
		if (httpStatus != null) {
			return httpStatus.getReasonPhrase();
		}

		return statusCode.toString();
	}

	private String resolveMessage(Throwable ex) {
		if (ex instanceof ResponseStatusException responseStatusException
				&& responseStatusException.getReason() != null
				&& !responseStatusException.getReason().isBlank()) {
			return responseStatusException.getReason();
		}

		if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
			return ex.getMessage();
		}

		return "Gateway request failed";
	}

	private String resolveCorrelationId(ServerWebExchange exchange) {
		Object correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
		if (correlationId instanceof String correlationIdValue && !correlationIdValue.isBlank()) {
			return correlationIdValue;
		}

		String headerValue = exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
		if (headerValue != null && !headerValue.isBlank()) {
			return headerValue.trim();
		}

		return null;
	}

	private byte[] serialize(GatewayErrorResponse response) {
		try {
			return objectMapper.writeValueAsBytes(response);
		} catch (JsonProcessingException ignored) {
			String fallbackBody = """
					{"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"%s}
					""".formatted(
					response.timestamp(),
					response.status(),
					response.error(),
					response.message(),
					response.path(),
					response.correlationId() == null ? "" : ",\"correlationId\":\"%s\"".formatted(response.correlationId())
			);
			return fallbackBody.getBytes(StandardCharsets.UTF_8);
		}
	}

}
