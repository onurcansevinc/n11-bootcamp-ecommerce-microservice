package com.ecommerce.microservices.cart_service.catalog.client;

import com.ecommerce.microservices.cart_service.catalog.dto.ProductSummary;

public interface ProductCatalogClient {

	ProductSummary getRequiredActiveProduct(Long productId);
}
