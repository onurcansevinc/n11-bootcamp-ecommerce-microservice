package com.ecommerce.microservices.payment_service.payment.gateway;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;

public interface PaymentGatewayAdapter {

	PaymentProvider provider();

	PaymentInitiationResult initiate(PaymentGatewayRequest request);

}
