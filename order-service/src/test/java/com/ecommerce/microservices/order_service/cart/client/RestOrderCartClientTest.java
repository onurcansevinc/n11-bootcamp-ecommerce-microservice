package com.ecommerce.microservices.order_service.cart.client;

import com.ecommerce.microservices.order_service.cart.dto.CartSummary;
import com.ecommerce.microservices.order_service.order.exception.CartAccessDeniedForOrderException;
import com.ecommerce.microservices.order_service.order.exception.CartNotFoundForOrderException;
import com.ecommerce.microservices.order_service.order.exception.CartServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestOrderCartClientTest {

	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
	private final RestOrderCartClient client =
			new RestOrderCartClient(restTemplate, "http://cart-service");

	@Test
	void getRequiredCartReturnsCartSummaryForSuccessfulResponse() {
		server.expect(requestTo("http://cart-service/api/v1/carts/cart-1"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer user-token"))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "id": "cart-1",
						    "customerId": "customer-1",
						    "status": "ACTIVE",
						    "items": [],
						    "totalAmount": 250.00
						  }
						}
						""", MediaType.APPLICATION_JSON));

		CartSummary cart = client.getRequiredCart("cart-1", "user-token");

		assertThat(cart.id()).isEqualTo("cart-1");
		assertThat(cart.customerId()).isEqualTo("customer-1");
		assertThat(cart.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(250));
		server.verify();
	}

	@Test
	void getRequiredCartMapsNotFoundToDomainException() {
		server.expect(requestTo("http://cart-service/api/v1/carts/cart-404"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> client.getRequiredCart("cart-404", "user-token"))
				.isInstanceOf(CartNotFoundForOrderException.class)
				.hasMessage("Cart not found with id: cart-404");

		server.verify();
	}

	@Test
	void getRequiredCartMapsForbiddenToDomainException() {
		server.expect(requestTo("http://cart-service/api/v1/carts/cart-1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThatThrownBy(() -> client.getRequiredCart("cart-1", "user-token"))
				.isInstanceOf(CartAccessDeniedForOrderException.class)
				.hasMessage("You do not have access to cart: cart-1");

		server.verify();
	}

	@Test
	void getRequiredCartMapsServerErrorsToServiceUnavailableException() {
		server.expect(requestTo("http://cart-service/api/v1/carts/cart-1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> client.getRequiredCart("cart-1", "user-token"))
				.isInstanceOf(CartServiceUnavailableException.class)
				.hasMessage("Cart service is unavailable");

		server.verify();
	}
}
