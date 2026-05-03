package com.ecommerce.microservices.api_gateway.common.error;

import com.ecommerce.microservices.api_gateway.common.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayErrorWebExceptionHandlerTest {

	private final GatewayErrorWebExceptionHandler handler =
			new GatewayErrorWebExceptionHandler(new ObjectMapper());

	@Test
	void handleWritesProblemBodyUsingResponseStatusExceptionAndCorrelationId() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/orders")
						.header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-1")
						.build()
		);

		handler.handle(exchange, new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Downstream failed")).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
				.isEqualTo("corr-1");
		assertThat(exchange.getResponse().getBodyAsString().block())
				.contains("\"status\":502")
				.contains("\"error\":\"Bad Gateway\"")
				.contains("\"message\":\"Downstream failed\"")
				.contains("\"path\":\"/api/v1/orders\"")
				.contains("\"correlationId\":\"corr-1\"");
	}

	@Test
	void handleFallsBackToInternalServerErrorForGenericException() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/cart").build()
		);

		handler.handle(exchange, new RuntimeException()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(exchange.getResponse().getBodyAsString().block())
				.contains("\"status\":500")
				.contains("\"error\":\"Internal Server Error\"")
				.contains("\"message\":\"Gateway request failed\"")
				.contains("\"path\":\"/api/v1/cart\"");
	}

}
