package com.ecommerce.microservices.payment_service.order.client;

import com.ecommerce.microservices.common.web.security.ClientCredentialsAccessTokenProvider;
import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;
import com.ecommerce.microservices.payment_service.payment.exception.OrderAccessDeniedForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderNotFoundForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderServiceUnavailableForPaymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class RestPaymentOrderClientTest {

	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

	@Mock
	private ClientCredentialsAccessTokenProvider accessTokenProvider;

	@Test
	void getRequiredOrderReturnsOrderSummaryForSuccessfulResponse() {
		RestPaymentOrderClient client = new RestPaymentOrderClient(
				restTemplate,
				accessTokenProvider,
				"http://order-service"
		);

		server.expect(requestTo("http://order-service/api/v1/orders/order-1"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer user-token"))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "id": "order-1",
						    "customerId": "customer-1",
						    "status": "PENDING_PAYMENT",
						    "totalAmount": 250.00,
						    "items": []
						  }
						}
						""", MediaType.APPLICATION_JSON));

		OrderSummary order = client.getRequiredOrder("order-1", "user-token");

		assertThat(order.id()).isEqualTo("order-1");
		assertThat(order.customerId()).isEqualTo("customer-1");
		assertThat(order.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(250));
		server.verify();
	}

	@Test
	void getRequiredOrderMapsForbiddenToDomainException() {
		RestPaymentOrderClient client = new RestPaymentOrderClient(
				restTemplate,
				accessTokenProvider,
				"http://order-service"
		);

		server.expect(requestTo("http://order-service/api/v1/orders/order-1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThatThrownBy(() -> client.getRequiredOrder("order-1", "user-token"))
				.isInstanceOf(OrderAccessDeniedForPaymentException.class)
				.hasMessage("You are not allowed to access order order-1");

		server.verify();
	}

	@Test
	void getRequiredOrderMapsNotFoundToDomainException() {
		RestPaymentOrderClient client = new RestPaymentOrderClient(
				restTemplate,
				accessTokenProvider,
				"http://order-service"
		);

		server.expect(requestTo("http://order-service/api/v1/orders/order-404"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> client.getRequiredOrder("order-404", "user-token"))
				.isInstanceOf(OrderNotFoundForPaymentException.class)
				.hasMessage("Order not found with id order-404");

		server.verify();
	}

	@Test
	void markPaymentSucceededUsesInternalBearerToken() {
		RestPaymentOrderClient client = new RestPaymentOrderClient(
				restTemplate,
				accessTokenProvider,
				"http://order-service"
		);

		when(accessTokenProvider.getAccessToken()).thenReturn("internal-token");

		server.expect(requestTo("http://order-service/internal/orders/order-1/payment-success"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer internal-token"))
				.andRespond(withSuccess());

		client.markPaymentSucceeded("order-1");

		server.verify();
	}

	@Test
	void markPaymentFailedMapsInternalAuthErrorsToServiceUnavailableException() {
		RestPaymentOrderClient client = new RestPaymentOrderClient(
				restTemplate,
				accessTokenProvider,
				"http://order-service"
		);

		when(accessTokenProvider.getAccessToken()).thenThrow(new IllegalStateException("token unavailable"));

		assertThatThrownBy(() -> client.markPaymentFailed("order-1"))
				.isInstanceOf(OrderServiceUnavailableForPaymentException.class)
				.hasMessage("Internal auth token could not be obtained");
	}
}
