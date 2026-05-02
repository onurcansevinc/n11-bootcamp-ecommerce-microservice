package com.ecommerce.microservices.payment_service.common.config;

import com.ecommerce.microservices.common.events.payment.PaymentEventTopology;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

	@Bean
	TopicExchange paymentEventsExchange(
			@Value("${payment.events.exchange:" + PaymentEventTopology.EXCHANGE + "}") String exchangeName
	) {
		return new TopicExchange(exchangeName, true, false);
	}

}
