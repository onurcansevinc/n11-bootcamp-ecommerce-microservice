package com.ecommerce.microservices.api_gateway.common.security;

import com.ecommerce.microservices.api_gateway.common.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySecurityHandlersTest {

	private final GatewayAuthenticationEntryPoint authenticationEntryPoint =
			new GatewayAuthenticationEntryPoint(new ObjectMapper());
	private final GatewayAccessDeniedHandler accessDeniedHandler =
			new GatewayAccessDeniedHandler(new ObjectMapper());

	@Test
	void authenticationEntryPointReturnsUnauthorizedJsonResponse() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/orders")
						.header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-auth")
						.build()
		);

		authenticationEntryPoint.commence(
				exchange,
				new InsufficientAuthenticationException("Missing token")
		).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
				.isEqualTo("corr-auth");
		assertThat(exchange.getResponse().getBodyAsString().block())
				.contains("\"status\":401")
				.contains("\"message\":\"Authentication is required to access this resource\"")
				.contains("\"correlationId\":\"corr-auth\"");
	}

	@Test
	void accessDeniedHandlerReturnsForbiddenJsonResponse() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/admin")
						.header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-denied")
						.build()
		);

		accessDeniedHandler.handle(exchange, new AccessDeniedException("Forbidden")).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
				.isEqualTo("corr-denied");
		assertThat(exchange.getResponse().getBodyAsString().block())
				.contains("\"status\":403")
				.contains("\"message\":\"You do not have permission to access this resource\"")
				.contains("\"path\":\"/api/v1/admin\"")
				.contains("\"correlationId\":\"corr-denied\"");
	}

}
