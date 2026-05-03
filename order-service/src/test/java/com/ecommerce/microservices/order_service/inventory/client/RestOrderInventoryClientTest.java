package com.ecommerce.microservices.order_service.inventory.client;

import com.ecommerce.microservices.common.web.security.ClientCredentialsAccessTokenProvider;
import com.ecommerce.microservices.order_service.inventory.dto.InventoryReservationSummary;
import com.ecommerce.microservices.order_service.order.exception.InventoryReservationFailedException;
import com.ecommerce.microservices.order_service.order.exception.InventoryServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class RestOrderInventoryClientTest {

	private final RestTemplate restTemplate = new RestTemplate();
	private final MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

	@Mock
	private ClientCredentialsAccessTokenProvider accessTokenProvider;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void createReservationReturnsReservationForSuccessfulResponse() {
		RestOrderInventoryClient client = new RestOrderInventoryClient(
				restTemplate,
				objectMapper,
				accessTokenProvider,
				"http://inventory-service"
		);

		when(accessTokenProvider.getAccessToken()).thenReturn("internal-token");

		server.expect(requestTo("http://inventory-service/api/v1/inventory/reservations"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer internal-token"))
				.andExpect(content().json("""
						{
						  "productId": 101,
						  "quantity": 2
						}
						"""))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "reservationCode": "res-1",
						    "productId": 101,
						    "quantity": 2,
						    "status": "RESERVED"
						  }
						}
						""", MediaType.APPLICATION_JSON));

		InventoryReservationSummary reservation = client.createReservation(101L, 2);

		assertThat(reservation.reservationCode()).isEqualTo("res-1");
		assertThat(reservation.productId()).isEqualTo(101L);
		assertThat(reservation.quantity()).isEqualTo(2);
		server.verify();
	}

	@Test
	void createReservationMapsRemoteProblemDetailFromClientErrors() {
		RestOrderInventoryClient client = new RestOrderInventoryClient(
				restTemplate,
				objectMapper,
				accessTokenProvider,
				"http://inventory-service"
		);

		when(accessTokenProvider.getAccessToken()).thenReturn("internal-token");

		server.expect(requestTo("http://inventory-service/api/v1/inventory/reservations"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.CONFLICT)
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{
								  "detail": "Inventory item not found for product id 101"
								}
								"""));

		assertThatThrownBy(() -> client.createReservation(101L, 2))
				.isInstanceOf(InventoryReservationFailedException.class)
				.hasMessage("Inventory item not found for product id 101");

		server.verify();
	}

	@Test
	void confirmReservationMapsTransportErrorsToServiceUnavailableException() {
		RestOrderInventoryClient client = new RestOrderInventoryClient(
				restTemplate,
				objectMapper,
				accessTokenProvider,
				"http://inventory-service"
		);

		when(accessTokenProvider.getAccessToken()).thenReturn("internal-token");

		server.expect(requestTo("http://inventory-service/api/v1/inventory/reservations/res-1/confirm"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer internal-token"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> client.confirmReservation("res-1"))
				.isInstanceOf(InventoryServiceUnavailableException.class)
				.hasMessage("Inventory reservation confirmation failed");

		server.verify();
	}

	@Test
	void releaseReservationMapsInternalAuthFailuresToServiceUnavailableException() {
		RestOrderInventoryClient client = new RestOrderInventoryClient(
				restTemplate,
				objectMapper,
				accessTokenProvider,
				"http://inventory-service"
		);

		when(accessTokenProvider.getAccessToken()).thenThrow(new IllegalStateException("token unavailable"));

		assertThatThrownBy(() -> client.releaseReservation("res-1"))
				.isInstanceOf(InventoryServiceUnavailableException.class)
				.hasMessage("Internal auth token could not be obtained");
	}
}
