package com.ecommerce.microservices.api_gateway.common.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void filterPropagatesIncomingCorrelationIdToRequestResponseAndAttributes() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/products")
						.header(CorrelationIdFilter.CORRELATION_ID_HEADER, " corr-123 ")
						.build()
		);
		AtomicReference<ServerWebExchange> seenExchange = new AtomicReference<>();
		GatewayFilterChain chain = mutatedExchange -> {
			seenExchange.set(mutatedExchange);
			return Mono.empty();
		};

		filter.filter(exchange, chain).block();

		assertThat(seenExchange.get()).isNotNull();
		assertThat(seenExchange.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
				.isEqualTo("corr-123");
		assertThat((String) seenExchange.get().getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
				.isEqualTo("corr-123");
		assertThat(seenExchange.get().getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
				.isEqualTo("corr-123");
	}

	@Test
	void filterGeneratesCorrelationIdWhenHeaderIsMissing() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/products").build()
		);
		AtomicReference<ServerWebExchange> seenExchange = new AtomicReference<>();

		filter.filter(exchange, mutatedExchange -> {
			seenExchange.set(mutatedExchange);
			return Mono.empty();
		}).block();

		String correlationId = seenExchange.get().getRequest().getHeaders()
				.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
		assertThat(correlationId).isNotBlank();
		assertThat((String) seenExchange.get().getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
				.isEqualTo(correlationId);
		assertThat(seenExchange.get().getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
				.isEqualTo(correlationId);
	}

}
