package com.ecommerce.microservices.api_gateway.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.core.convert.converter.Converter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(
			ServerHttpSecurity http,
			GatewayAuthenticationEntryPoint authenticationEntryPoint,
			GatewayAccessDeniedHandler accessDeniedHandler,
			Converter<Jwt, Mono<org.springframework.security.authentication.AbstractAuthenticationToken>> jwtAuthenticationConverter
	) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/inventory/**")
						.hasAnyAuthority("SCOPE_inventory.read", "SCOPE_inventory.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers("/api/v1/carts/**").authenticated()
						.pathMatchers("/api/v1/orders/**").authenticated()
						.pathMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.PUT, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.PATCH, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.DELETE, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.POST, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.PUT, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.PATCH, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.DELETE, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.POST, "/api/v1/inventory/**")
						.hasAnyAuthority("SCOPE_inventory.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.pathMatchers(HttpMethod.PUT, "/api/v1/inventory/**")
						.hasAnyAuthority("SCOPE_inventory.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.anyExchange().authenticated()
				)
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				)
				.build();
	}

	@Bean
	Converter<Jwt, Mono<org.springframework.security.authentication.AbstractAuthenticationToken>> jwtAuthenticationConverter(
			KeycloakJwtAuthoritiesConverter authoritiesConverter
	) {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
	}

}
