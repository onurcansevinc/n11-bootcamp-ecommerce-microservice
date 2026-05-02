package com.ecommerce.microservices.order_service.cart.client;

import com.ecommerce.microservices.order_service.cart.dto.CartSummary;
import com.ecommerce.microservices.order_service.order.exception.CartAccessDeniedForOrderException;
import com.ecommerce.microservices.order_service.order.exception.CartNotFoundForOrderException;
import com.ecommerce.microservices.order_service.order.exception.CartServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class RestOrderCartClient implements OrderCartClient {

	private final RestTemplate restTemplate;
	private final String cartServiceBaseUrl;

	public RestOrderCartClient(
			@Qualifier("serviceRestTemplate") RestTemplate restTemplate,
			@Value("${clients.cart-service.base-url:http://cart-service}") String cartServiceBaseUrl
	) {
		this.restTemplate = restTemplate;
		this.cartServiceBaseUrl = cartServiceBaseUrl;
	}

	@Override
	public CartSummary getRequiredCart(String cartId, String bearerToken) {
		try {
			ResponseEntity<CartLookupResponse> response = restTemplate.exchange(
					cartServiceBaseUrl + "/api/v1/carts/{cartId}",
					HttpMethod.GET,
					authorizedEntity(bearerToken),
					CartLookupResponse.class,
					cartId
			);

			if (response.getBody() == null || response.getBody().data() == null) {
				throw new CartNotFoundForOrderException(cartId);
			}

			return response.getBody().data();
		} catch (HttpClientErrorException.NotFound exception) {
			throw new CartNotFoundForOrderException(cartId);
		} catch (HttpClientErrorException.Forbidden exception) {
			throw new CartAccessDeniedForOrderException(cartId);
		} catch (RestClientException exception) {
			throw new CartServiceUnavailableException("Cart service is unavailable", exception);
		}
	}

	private HttpEntity<Void> authorizedEntity(String bearerToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(bearerToken);
		return new HttpEntity<>(headers);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record CartLookupResponse(CartSummary data) {
	}

}
