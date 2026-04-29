package com.ecommerce.microservices.inventory_service.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler,
			JwtAuthenticationConverter jwtAuthenticationConverter
	) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/inventory/**")
						.hasAnyAuthority("SCOPE_inventory.read", "SCOPE_inventory.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.POST, "/api/v1/inventory/**")
						.hasAnyAuthority("SCOPE_inventory.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.PUT, "/api/v1/inventory/**")
						.hasAnyAuthority("SCOPE_inventory.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.anyRequest().authenticated()
				)
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				);

		return http.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakJwtAuthoritiesConverter authoritiesConverter) {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return jwtAuthenticationConverter;
	}

}
