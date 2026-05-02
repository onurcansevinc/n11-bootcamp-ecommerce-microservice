package com.ecommerce.microservices.payment_service.common.config;

import com.ecommerce.microservices.common.web.security.ClientCredentialsAccessTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

@Configuration
public class HttpClientConfig {

	@Bean
	@LoadBalanced
	RestTemplate serviceRestTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Bean
	RestTemplate authRestTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	ClientCredentialsAccessTokenProvider internalServiceAccessTokenProvider(
			@Qualifier("authRestTemplate") RestTemplate authRestTemplate,
			Clock clock,
			@Value("${clients.internal-auth.token-url:http://localhost:8080/realms/ecommerce/protocol/openid-connect/token}") String tokenUrl,
			@Value("${clients.internal-auth.client-id:service-internal}") String clientId,
			@Value("${clients.internal-auth.client-secret:change-me-service-client-secret}") String clientSecret
	) {
		return new ClientCredentialsAccessTokenProvider(
				authRestTemplate,
				clock,
				tokenUrl,
				clientId,
				clientSecret
		);
	}

}
