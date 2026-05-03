package com.ecommerce.microservices.api_gateway.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthoritiesConverterTest {

	private final KeycloakJwtAuthoritiesConverter converter = new KeycloakJwtAuthoritiesConverter();

	@Test
	void convertIncludesScopesAndNormalizedRealmRoles() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("scope", "openid profile")
				.claim("realm_access", Map.of("roles", List.of("service-internal", "order-manager")))
				.build();

		Collection<GrantedAuthority> authorities = converter.convert(jwt);

		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder(
						"SCOPE_openid",
						"SCOPE_profile",
						"ROLE_SERVICE_INTERNAL",
						"ROLE_ORDER_MANAGER"
				);
	}

	@Test
	void convertReturnsOnlyScopesWhenRealmAccessIsMissing() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("scope", "email")
				.build();

		Collection<GrantedAuthority> authorities = converter.convert(jwt);

		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("SCOPE_email");
	}

}
