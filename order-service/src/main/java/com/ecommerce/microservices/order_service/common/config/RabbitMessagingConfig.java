package com.ecommerce.microservices.order_service.common.config;

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
			@Value("${order.payment.events.exchange:" + PaymentEventTopology.EXCHANGE + "}") String exchangeName
	) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	TopicExchange paymentEventsDeadLetterExchange(
			@Value("${order.payment.events.dlx:" + PaymentEventTopology.DEAD_LETTER_EXCHANGE + "}") String exchangeName
	) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	Queue orderPaymentEventsQueue(
			@Value("${order.payment.events.queue:" + PaymentEventTopology.ORDER_QUEUE + "}") String queueName,
			@Value("${order.payment.events.dlx:" + PaymentEventTopology.DEAD_LETTER_EXCHANGE + "}") String deadLetterExchange
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
	Queue orderPaymentEventsDeadLetterQueue(
			@Value("${order.payment.events.dlq:" + PaymentEventTopology.ORDER_DLQ + "}") String queueName
	) {
		return new Queue(queueName, true);
	}

	@Bean
	Binding orderPaymentSucceededBinding(
			Queue orderPaymentEventsQueue,
			TopicExchange paymentEventsExchange
	) {
		return BindingBuilder.bind(orderPaymentEventsQueue)
				.to(paymentEventsExchange)
				.with(PaymentEventTypes.SUCCEEDED);
	}

	@Bean
	Binding orderPaymentFailedBinding(
			Queue orderPaymentEventsQueue,
			TopicExchange paymentEventsExchange
	) {
		return BindingBuilder.bind(orderPaymentEventsQueue)
				.to(paymentEventsExchange)
				.with(PaymentEventTypes.FAILED);
	}

	@Bean
	Binding orderPaymentDeadLetterBinding(
			Queue orderPaymentEventsDeadLetterQueue,
			TopicExchange paymentEventsDeadLetterExchange,
			@Value("${order.payment.events.queue:" + PaymentEventTopology.ORDER_QUEUE + "}") String queueName
	) {
		return BindingBuilder.bind(orderPaymentEventsDeadLetterQueue)
				.to(paymentEventsDeadLetterExchange)
				.with(queueName);
	}

}
