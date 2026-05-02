package com.ecommerce.microservices.common.web.security;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;

public class ClientCredentialsAccessTokenProvider {

	private static final long EXPIRY_SKEW_SECONDS = 30;

	private final RestTemplate restTemplate;
	private final Clock clock;
	private final String tokenUrl;
	private final String clientId;
	private final String clientSecret;

	private CachedAccessToken cachedAccessToken;

	public ClientCredentialsAccessTokenProvider(
			RestTemplate restTemplate,
			Clock clock,
			String tokenUrl,
			String clientId,
			String clientSecret
	) {
		this.restTemplate = restTemplate;
		this.clock = clock;
		this.tokenUrl = tokenUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public synchronized String getAccessToken() {
		if (cachedAccessToken != null && !cachedAccessToken.isExpired(clock)) {
			return cachedAccessToken.token();
		}

		TokenResponse tokenResponse = requestAccessToken();
		Instant expiresAt = Instant.now(clock)
				.plusSeconds(Math.max(1, tokenResponse.expiresIn() - EXPIRY_SKEW_SECONDS));

		cachedAccessToken = new CachedAccessToken(tokenResponse.accessToken(), expiresAt);
		return cachedAccessToken.token();
	}

	private TokenResponse requestAccessToken() {
		try {
			ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
					tokenUrl,
					formEntity(),
					TokenResponse.class
			);

			if (response.getBody() == null || response.getBody().accessToken() == null) {
				throw new IllegalStateException("Internal auth token response is empty");
			}

			return response.getBody();
		} catch (RestClientException exception) {
			throw new IllegalStateException("Internal auth token could not be obtained", exception);
		}
	}

	private HttpEntity<MultiValueMap<String, String>> formEntity() {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "client_credentials");
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		return new HttpEntity<>(body, headers);
	}

	private record TokenResponse(String access_token, Long expires_in) {
		String accessToken() {
			return access_token;
		}

		long expiresIn() {
			return expires_in == null ? 60L : expires_in;
		}
	}

	private record CachedAccessToken(String token, Instant expiresAt) {
		boolean isExpired(Clock clock) {
			return expiresAt.isBefore(Instant.now(clock));
		}
	}

}
