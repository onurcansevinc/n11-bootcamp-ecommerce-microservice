package com.ecommerce.microservices.product_service.common.security;

import com.ecommerce.microservices.common.web.security.CommonServletSecurityConfiguration;
import com.ecommerce.microservices.common.web.security.KeycloakJwtAuthoritiesConverter;
import com.ecommerce.microservices.common.web.security.RestAccessDeniedHandler;
import com.ecommerce.microservices.common.web.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Import(CommonServletSecurityConfiguration.class)
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
						.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.PUT, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/products", "/api/v1/products/**")
						.hasAnyAuthority("SCOPE_products.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.POST, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.PUT, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/categories", "/api/v1/categories/**")
						.hasAnyAuthority("SCOPE_categories.write", "ROLE_ADMIN", "ROLE_CATALOG_MANAGER")
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
