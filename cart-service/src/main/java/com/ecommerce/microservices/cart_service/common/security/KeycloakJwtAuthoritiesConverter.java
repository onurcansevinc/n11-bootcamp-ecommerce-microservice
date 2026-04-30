package com.ecommerce.microservices.cart_service.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

@Component
public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final JwtGrantedAuthoritiesConverter scopesConverter;

	public KeycloakJwtAuthoritiesConverter() {
		this.scopesConverter = new JwtGrantedAuthoritiesConverter();
		this.scopesConverter.setAuthoritiesClaimName("scope");
		this.scopesConverter.setAuthorityPrefix("SCOPE_");
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new LinkedHashSet<>(scopesConverter.convert(jwt));
		extractRealmRoles(jwt).forEach(authorities::add);
		return authorities;
	}

	@SuppressWarnings("unchecked")
	private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new LinkedHashSet<>();
		Map<String, Object> realmAccess = jwt.getClaim("realm_access");
		if (realmAccess == null) {
			return authorities;
		}

		Object roles = realmAccess.get("roles");
		if (!(roles instanceof Collection<?> roleNames)) {
			return authorities;
		}

		for (Object roleName : roleNames) {
			if (roleName instanceof String role && !role.isBlank()) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizeRole(role)));
			}
		}

		return authorities;
	}

	private String normalizeRole(String role) {
		return role.trim().replace('-', '_').toUpperCase(Locale.ROOT);
	}

}
