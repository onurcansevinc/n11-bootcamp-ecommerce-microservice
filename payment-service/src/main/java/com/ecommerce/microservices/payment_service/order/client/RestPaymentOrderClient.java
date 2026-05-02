package com.ecommerce.microservices.payment_service.order.client;

import com.ecommerce.microservices.common.web.security.ClientCredentialsAccessTokenProvider;
import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;
import com.ecommerce.microservices.payment_service.payment.exception.OrderAccessDeniedForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderNotFoundForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderServiceUnavailableForPaymentException;
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
public class RestPaymentOrderClient implements PaymentOrderClient {

	private final RestTemplate restTemplate;
	private final ClientCredentialsAccessTokenProvider accessTokenProvider;
	private final String orderServiceBaseUrl;

	public RestPaymentOrderClient(
			@Qualifier("serviceRestTemplate") RestTemplate restTemplate,
			ClientCredentialsAccessTokenProvider accessTokenProvider,
			@Value("${clients.order-service.base-url:http://order-service}") String orderServiceBaseUrl
	) {
		this.restTemplate = restTemplate;
		this.accessTokenProvider = accessTokenProvider;
		this.orderServiceBaseUrl = orderServiceBaseUrl;
	}

	@Override
	public OrderSummary getRequiredOrder(String orderId, String bearerToken) {
		try {
			ResponseEntity<OrderLookupResponse> response = restTemplate.exchange(
					orderServiceBaseUrl + "/api/v1/orders/{orderId}",
					HttpMethod.GET,
					authorizedEntity(null, bearerToken),
					OrderLookupResponse.class,
					orderId
			);

			if (response.getBody() == null || response.getBody().data() == null) {
				throw new OrderNotFoundForPaymentException(orderId);
			}

			return response.getBody().data();
		} catch (HttpClientErrorException.NotFound exception) {
			throw new OrderNotFoundForPaymentException(orderId);
		} catch (HttpClientErrorException.Forbidden exception) {
			throw new OrderAccessDeniedForPaymentException(orderId);
		} catch (RestClientException exception) {
			throw new OrderServiceUnavailableForPaymentException("Order service is unavailable", exception);
		}
	}

	@Override
	public void markPaymentSucceeded(String orderId) {
		callInternalTransition(orderId, "/internal/orders/{orderId}/payment-success");
	}

	@Override
	public void markPaymentFailed(String orderId) {
		callInternalTransition(orderId, "/internal/orders/{orderId}/payment-failure");
	}

	private void callInternalTransition(String orderId, String path) {
		try {
			restTemplate.exchange(
					orderServiceBaseUrl + path,
					HttpMethod.POST,
					internalAuthorizedEntity(),
					Void.class,
					orderId
			);
		} catch (IllegalStateException exception) {
			throw new OrderServiceUnavailableForPaymentException("Internal auth token could not be obtained", exception);
		} catch (RestClientException exception) {
			throw new OrderServiceUnavailableForPaymentException("Order service transition failed", exception);
		}
	}

	private <T> HttpEntity<T> authorizedEntity(T body, String bearerToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(bearerToken);
		return new HttpEntity<>(body, headers);
	}

	private HttpEntity<Void> internalAuthorizedEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessTokenProvider.getAccessToken());
		return new HttpEntity<>(headers);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OrderLookupResponse(OrderSummary data) {
	}

}
