package com.ecommerce.microservices.common.events.payment;

public final class PaymentEventTopology {

	public static final String EXCHANGE = "payment.events";
	public static final String DEAD_LETTER_EXCHANGE = "payment.events.dlx";
	public static final String ORDER_QUEUE = "order.payment-events.v1";
	public static final String ORDER_DLQ = "order.payment-events.dlq.v1";
	public static final String NOTIFICATION_QUEUE = "notification.payment-events.v1";
	public static final String NOTIFICATION_DLQ = "notification.payment-events.dlq.v1";

	private PaymentEventTopology() {
	}
}
