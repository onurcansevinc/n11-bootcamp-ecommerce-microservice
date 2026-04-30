package com.ecommerce.microservices.order_service.inventory.client;

import com.ecommerce.microservices.order_service.inventory.dto.InventoryReservationSummary;
import com.ecommerce.microservices.order_service.order.exception.InventoryReservationFailedException;
import com.ecommerce.microservices.order_service.order.exception.InventoryServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestOrderInventoryClient implements OrderInventoryClient {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final String inventoryServiceBaseUrl;

	public RestOrderInventoryClient(
			RestTemplate restTemplate,
			ObjectMapper objectMapper,
			@Value("${clients.inventory-service.base-url:http://inventory-service}") String inventoryServiceBaseUrl
	) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.inventoryServiceBaseUrl = inventoryServiceBaseUrl;
	}

	@Override
	public InventoryReservationSummary createReservation(Long productId, Integer quantity, String bearerToken) {
		try {
			ResponseEntity<ReservationLookupResponse> response = restTemplate.exchange(
					inventoryServiceBaseUrl + "/api/v1/inventory/reservations",
					HttpMethod.POST,
					authorizedEntity(new CreateInventoryReservationPayload(productId, quantity), bearerToken),
					ReservationLookupResponse.class
			);

			if (response.getBody() == null || response.getBody().data() == null) {
				throw new InventoryReservationFailedException(
						"Reservation could not be created for product " + productId
				);
			}

			return response.getBody().data();
		} catch (HttpClientErrorException exception) {
			throw new InventoryReservationFailedException(resolveProblemDetail(exception));
		} catch (RestClientException exception) {
			throw new InventoryServiceUnavailableException("Inventory service is unavailable", exception);
		}
	}

	@Override
	public void releaseReservation(String reservationCode, String bearerToken) {
		try {
			restTemplate.exchange(
					inventoryServiceBaseUrl + "/api/v1/inventory/reservations/{reservationCode}/release",
					HttpMethod.POST,
					authorizedEntity(null, bearerToken),
					Void.class,
					reservationCode
			);
		} catch (RestClientException exception) {
			throw new InventoryServiceUnavailableException("Inventory reservation release failed", exception);
		}
	}

	private String resolveProblemDetail(HttpClientErrorException exception) {
		try {
			RemoteProblemDetail remoteProblemDetail = objectMapper.readValue(
					exception.getResponseBodyAsString(),
					RemoteProblemDetail.class
			);
			if (remoteProblemDetail.detail() != null && !remoteProblemDetail.detail().isBlank()) {
				return remoteProblemDetail.detail();
			}
		} catch (Exception ignored) {
			// fall back to a generic message when remote body is not parseable
		}

		return "Inventory reservation request failed with status " + exception.getStatusCode().value();
	}

	private <T> HttpEntity<T> authorizedEntity(T body, String bearerToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(bearerToken);
		return new HttpEntity<>(body, headers);
	}

	private record CreateInventoryReservationPayload(Long productId, Integer quantity) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ReservationLookupResponse(InventoryReservationSummary data) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RemoteProblemDetail(String detail) {
	}

}
