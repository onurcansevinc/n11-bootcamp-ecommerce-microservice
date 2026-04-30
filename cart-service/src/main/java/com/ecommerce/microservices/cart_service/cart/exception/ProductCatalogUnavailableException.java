package com.ecommerce.microservices.cart_service.cart.exception;

public class ProductCatalogUnavailableException extends RuntimeException {

	public ProductCatalogUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
