package com.ecommerce.microservices.cart_service.catalog.client;

import com.ecommerce.microservices.cart_service.cart.exception.InactiveProductException;
import com.ecommerce.microservices.cart_service.cart.exception.ProductCatalogUnavailableException;
import com.ecommerce.microservices.cart_service.cart.exception.ProductNotFoundForCartException;
import com.ecommerce.microservices.cart_service.catalog.dto.ProductSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestProductCatalogClient implements ProductCatalogClient {

	private final RestTemplate restTemplate;
	private final String productServiceBaseUrl;

	public RestProductCatalogClient(
			RestTemplate restTemplate,
			@Value("${clients.product-service.base-url:http://product-service}") String productServiceBaseUrl
	) {
		this.restTemplate = restTemplate;
		this.productServiceBaseUrl = productServiceBaseUrl;
	}

	@Override
	public ProductSummary getRequiredActiveProduct(Long productId) {
		try {
			ProductLookupResponse response = restTemplate.getForObject(
					productServiceBaseUrl + "/api/v1/products/{productId}",
					ProductLookupResponse.class,
					productId
			);

			if (response == null || response.data() == null) {
				throw new ProductNotFoundForCartException(productId);
			}

			ProductSummary product = response.data();
			if (!Boolean.TRUE.equals(product.active())) {
				throw new InactiveProductException(productId);
			}

			return product;
		} catch (HttpClientErrorException exception) {
			if (exception.getStatusCode().value() == HttpStatusCode.valueOf(404).value()) {
				throw new ProductNotFoundForCartException(productId);
			}
			throw new ProductCatalogUnavailableException("Product service returned an unexpected error", exception);
		} catch (RestClientException exception) {
			throw new ProductCatalogUnavailableException("Product service is unavailable", exception);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ProductLookupResponse(ProductSummary data) {
	}

}
