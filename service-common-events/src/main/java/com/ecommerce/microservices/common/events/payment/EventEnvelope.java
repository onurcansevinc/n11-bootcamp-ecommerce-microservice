package com.ecommerce.microservices.common.events.payment;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
		String eventId,
		String eventType,
		String version,
		Instant occurredAt,
		T payload
) {
}
