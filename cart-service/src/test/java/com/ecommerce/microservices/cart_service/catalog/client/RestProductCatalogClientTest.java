package com.ecommerce.microservices.cart_service.catalog.client;

import com.ecommerce.microservices.cart_service.cart.exception.InactiveProductException;
import com.ecommerce.microservices.cart_service.cart.exception.ProductCatalogUnavailableException;
import com.ecommerce.microservices.cart_service.cart.exception.ProductNotFoundForCartException;
import com.ecommerce.microservices.cart_service.catalog.dto.ProductSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestProductCatalogClientTest {

	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
	private final RestProductCatalogClient client =
			new RestProductCatalogClient(restTemplate, "http://product-service");

	@Test
	void getRequiredActiveProductReturnsProductForActiveResponse() {
		server.expect(requestTo("http://product-service/api/v1/products/1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "id": 1,
						    "name": "Keyboard",
						    "price": 249.90,
						    "active": true,
						    "ignoredField": "extra"
						  }
						}
						""", MediaType.APPLICATION_JSON));

		ProductSummary product = client.getRequiredActiveProduct(1L);

		assertThat(product.id()).isEqualTo(1L);
		assertThat(product.name()).isEqualTo("Keyboard");
		assertThat(product.price()).isEqualByComparingTo(BigDecimal.valueOf(249.90));
		assertThat(product.active()).isTrue();
		server.verify();
	}

	@Test
	void getRequiredActiveProductTreatsMissingDataAsProductNotFound() {
		server.expect(requestTo("http://product-service/api/v1/products/1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "data": null
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getRequiredActiveProduct(1L))
				.isInstanceOf(ProductNotFoundForCartException.class)
				.hasMessage("Product with id 1 was not found");

		server.verify();
	}

	@Test
	void getRequiredActiveProductRejectsInactiveProduct() {
		server.expect(requestTo("http://product-service/api/v1/products/2"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "id": 2,
						    "name": "Mouse",
						    "price": 99.90,
						    "active": false
						  }
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getRequiredActiveProduct(2L))
				.isInstanceOf(InactiveProductException.class)
				.hasMessage("Product with id 2 is not active");

		server.verify();
	}

	@Test
	void getRequiredActiveProductMapsNotFoundToDomainException() {
		server.expect(requestTo("http://product-service/api/v1/products/404"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> client.getRequiredActiveProduct(404L))
				.isInstanceOf(ProductNotFoundForCartException.class)
				.hasMessage("Product with id 404 was not found");

		server.verify();
	}

	@Test
	void getRequiredActiveProductMapsUnexpectedErrorsToCatalogUnavailable() {
		server.expect(requestTo("http://product-service/api/v1/products/1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST));

		assertThatThrownBy(() -> client.getRequiredActiveProduct(1L))
				.isInstanceOf(ProductCatalogUnavailableException.class)
				.hasMessage("Product service returned an unexpected error");

		server.verify();
	}

}
