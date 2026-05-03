package com.ecommerce.microservices.notification_service.common.config;

import com.ecommerce.microservices.common.events.payment.PaymentEventTopology;
import com.ecommerce.microservices.common.events.payment.PaymentEventTypes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMessagingConfig {

	@Bean
	TopicExchange paymentEventsExchange(
			@Value("${notification.payment.events.exchange:" + PaymentEventTopology.EXCHANGE + "}") String exchangeName
	) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	TopicExchange paymentEventsDeadLetterExchange(
			@Value("${notification.payment.events.dlx:" + PaymentEventTopology.DEAD_LETTER_EXCHANGE + "}") String exchangeName
	) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	Queue notificationPaymentEventsQueue(
			@Value("${notification.payment.events.queue:" + PaymentEventTopology.NOTIFICATION_QUEUE + "}") String queueName,
			@Value("${notification.payment.events.dlx:" + PaymentEventTopology.DEAD_LETTER_EXCHANGE + "}") String deadLetterExchange
	) {
		return new Queue(
				queueName,
				true,
				false,
				false,
				Map.of(
						"x-dead-letter-exchange", deadLetterExchange,
						"x-dead-letter-routing-key", queueName
				)
		);
	}

	@Bean
	Queue notificationPaymentEventsDeadLetterQueue(
			@Value("${notification.payment.events.dlq:" + PaymentEventTopology.NOTIFICATION_DLQ + "}") String queueName
	) {
		return new Queue(queueName, true);
	}

	@Bean
	Binding notificationPaymentSucceededBinding(
			Queue notificationPaymentEventsQueue,
			TopicExchange paymentEventsExchange
	) {
		return BindingBuilder.bind(notificationPaymentEventsQueue)
				.to(paymentEventsExchange)
				.with(PaymentEventTypes.SUCCEEDED);
	}

	@Bean
	Binding notificationPaymentFailedBinding(
			Queue notificationPaymentEventsQueue,
			TopicExchange paymentEventsExchange
	) {
		return BindingBuilder.bind(notificationPaymentEventsQueue)
				.to(paymentEventsExchange)
				.with(PaymentEventTypes.FAILED);
	}

	@Bean
	Binding notificationPaymentDeadLetterBinding(
			Queue notificationPaymentEventsDeadLetterQueue,
			TopicExchange paymentEventsDeadLetterExchange,
			@Value("${notification.payment.events.queue:" + PaymentEventTopology.NOTIFICATION_QUEUE + "}") String queueName
	) {
		return BindingBuilder.bind(notificationPaymentEventsDeadLetterQueue)
				.to(paymentEventsDeadLetterExchange)
				.with(queueName);
	}

}
