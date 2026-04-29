package com.ecommerce.microservices.api_gateway.common.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	public static final String CORRELATION_ID_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String correlationId = resolveCorrelationId(exchange);

		ServerHttpRequest mutatedRequest = exchange.getRequest()
				.mutate()
				.headers(headers -> headers.set(CORRELATION_ID_HEADER, correlationId))
				.build();

		ServerWebExchange mutatedExchange = exchange.mutate()
				.request(mutatedRequest)
				.build();

		mutatedExchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);
		mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

		return chain.filter(mutatedExchange);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private String resolveCorrelationId(ServerWebExchange exchange) {
		String incomingCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
		if (incomingCorrelationId != null && !incomingCorrelationId.isBlank()) {
			return incomingCorrelationId.trim();
		}

		return UUID.randomUUID().toString();
	}

}
