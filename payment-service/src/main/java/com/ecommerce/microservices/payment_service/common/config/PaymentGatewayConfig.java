package com.ecommerce.microservices.payment_service.common.config;

import com.iyzipay.Options;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IyzicoProperties.class)
public class PaymentGatewayConfig {

	@Bean
	Options iyzicoOptions(IyzicoProperties iyzicoProperties) {
		Options options = new Options();
		options.setApiKey(iyzicoProperties.apiKey());
		options.setSecretKey(iyzicoProperties.secretKey());
		options.setBaseUrl(iyzicoProperties.baseUrl());
		return options;
	}

}
