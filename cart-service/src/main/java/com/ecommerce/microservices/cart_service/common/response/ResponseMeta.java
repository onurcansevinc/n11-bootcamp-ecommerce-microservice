package com.ecommerce.microservices.cart_service.common.response;

import org.springframework.data.domain.Page;

public record ResponseMeta(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext,
		boolean hasPrevious
) {
	public static ResponseMeta from(Page<?> page) {
		return new ResponseMeta(
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext(),
				page.hasPrevious()
		);
	}
}
