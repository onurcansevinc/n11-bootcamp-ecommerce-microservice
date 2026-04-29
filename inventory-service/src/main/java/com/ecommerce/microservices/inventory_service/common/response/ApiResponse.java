package com.ecommerce.microservices.inventory_service.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		Instant timestamp,
		String message,
		T data,
		ResponseMeta meta
) {
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(Instant.now(), message, data, null);
	}

	public static <T> ApiResponse<T> success(String message, T data, ResponseMeta meta) {
		return new ApiResponse<>(Instant.now(), message, data, meta);
	}
}
