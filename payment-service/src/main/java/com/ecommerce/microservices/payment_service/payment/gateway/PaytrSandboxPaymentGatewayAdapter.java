package com.ecommerce.microservices.payment_service.payment.gateway;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import org.springframework.stereotype.Component;

@Component
public class PaytrSandboxPaymentGatewayAdapter implements PaymentGatewayAdapter {

	@Override
	public PaymentProvider provider() {
		return PaymentProvider.PAYTR;
	}

	@Override
	public PaymentInitiationResult initiate(PaymentGatewayRequest request) {
		String externalPaymentId = "paytr-" + java.util.UUID.randomUUID();
		String checkoutUrl = "https://sandbox.paytr.local/checkout/" + request.order().id() + "?paymentId=" + externalPaymentId;
		return new PaymentInitiationResult(externalPaymentId, checkoutUrl);
	}

}
