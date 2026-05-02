package com.ecommerce.microservices.payment_service.payment.gateway;

import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.exception.UnsupportedPaymentProviderException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayRegistry {

	private final Map<PaymentProvider, PaymentGatewayAdapter> adapters = new EnumMap<>(PaymentProvider.class);

	public PaymentGatewayRegistry(List<PaymentGatewayAdapter> paymentGatewayAdapters) {
		for (PaymentGatewayAdapter adapter : paymentGatewayAdapters) {
			adapters.put(adapter.provider(), adapter);
		}
	}

	public PaymentGatewayAdapter getRequired(PaymentProvider provider) {
		PaymentGatewayAdapter adapter = adapters.get(provider);
		if (adapter == null) {
			throw new UnsupportedPaymentProviderException(provider.name());
		}
		return adapter;
	}

}
