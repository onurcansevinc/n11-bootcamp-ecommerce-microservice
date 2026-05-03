package com.ecommerce.microservices.common.web.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

public class CurrentCustomerIdResolver {

	public String resolve(JwtAuthenticationToken authentication) {
		if (authentication == null || authentication.getToken() == null) {
			throw invalidToken();
		}

		Jwt jwt = authentication.getToken();
		return Stream.of(
						jwt.getSubject(),
						jwt.getClaimAsString("preferred_username"),
						jwt.getClaimAsString("email")
				)
				.filter(StringUtils::hasText)
				.findFirst()
				.orElseThrow(this::invalidToken);
	}

	private ResponseStatusException invalidToken() {
		return new ResponseStatusException(
				HttpStatus.UNAUTHORIZED,
				"Unable to resolve current user from access token"
		);
	}

}
