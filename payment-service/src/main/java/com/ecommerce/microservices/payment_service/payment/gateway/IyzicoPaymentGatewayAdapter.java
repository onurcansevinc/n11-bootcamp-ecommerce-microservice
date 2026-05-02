package com.ecommerce.microservices.payment_service.payment.gateway;

import com.ecommerce.microservices.payment_service.common.config.IyzicoProperties;
import com.ecommerce.microservices.payment_service.order.dto.OrderItemSummary;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentAddressRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentBuyerRequest;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentGatewayInitializationException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentGatewayVerificationException;
import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class IyzicoPaymentGatewayAdapter implements PaymentGatewayAdapter {

	private final Options iyzicoOptions;
	private final IyzicoProperties iyzicoProperties;

	public IyzicoPaymentGatewayAdapter(Options iyzicoOptions, IyzicoProperties iyzicoProperties) {
		this.iyzicoOptions = iyzicoOptions;
		this.iyzicoProperties = iyzicoProperties;
	}

	@Override
	public PaymentProvider provider() {
		return PaymentProvider.IYZICO;
	}

	@Override
	public PaymentInitiationResult initiate(PaymentGatewayRequest request) {
		CreateCheckoutFormInitializeRequest iyzicoRequest = new CreateCheckoutFormInitializeRequest();
		iyzicoRequest.setLocale(normalizeLocale(request.checkout().locale()));
		iyzicoRequest.setConversationId(request.order().id());
		iyzicoRequest.setPrice(request.order().totalAmount());
		iyzicoRequest.setPaidPrice(request.order().totalAmount());
		iyzicoRequest.setCurrency(Currency.TRY.name());
		iyzicoRequest.setBasketId(request.order().id());
		iyzicoRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());
		iyzicoRequest.setCallbackUrl(iyzicoProperties.callbackUrl());
		iyzicoRequest.setBuyer(toBuyer(
				request.customerId(),
				request.checkout().buyer(),
				request.checkout().billingAddress(),
				request.clientIp()
		));
		iyzicoRequest.setBillingAddress(toAddress(request.checkout().billingAddress()));
		iyzicoRequest.setShippingAddress(toAddress(request.checkout().shippingAddress()));
		iyzicoRequest.setBasketItems(toBasketItems(request.order().items()));

		CheckoutFormInitialize checkoutFormInitialize = CheckoutFormInitialize.create(iyzicoRequest, iyzicoOptions);
		if (!"success".equalsIgnoreCase(checkoutFormInitialize.getStatus())
				|| !StringUtils.hasText(checkoutFormInitialize.getToken())
				|| !StringUtils.hasText(checkoutFormInitialize.getPaymentPageUrl())) {
			String message = StringUtils.hasText(checkoutFormInitialize.getErrorMessage())
					? checkoutFormInitialize.getErrorMessage()
					: "Iyzico checkout form could not be initialized";
			throw new PaymentGatewayInitializationException(provider().name(), message);
		}

		return new PaymentInitiationResult(
				checkoutFormInitialize.getToken(),
				checkoutFormInitialize.getPaymentPageUrl()
		);
	}

	public PaymentVerificationResult retrieve(String token) {
		RetrieveCheckoutFormRequest retrieveRequest = new RetrieveCheckoutFormRequest();
		retrieveRequest.setLocale(Locale.TR.getValue());
		retrieveRequest.setConversationId(token);
		retrieveRequest.setToken(token);

		CheckoutForm checkoutForm = CheckoutForm.retrieve(retrieveRequest, iyzicoOptions);
		if (!"success".equalsIgnoreCase(checkoutForm.getStatus())) {
			String message = StringUtils.hasText(checkoutForm.getErrorMessage())
					? checkoutForm.getErrorMessage()
					: "Iyzico checkout form result could not be retrieved";
			throw new PaymentGatewayVerificationException(provider().name(), message);
		}

		if ("SUCCESS".equalsIgnoreCase(checkoutForm.getPaymentStatus())) {
			return PaymentVerificationResult.success();
		}

		String failureReason = StringUtils.hasText(checkoutForm.getErrorMessage())
				? checkoutForm.getErrorMessage()
				: "Iyzico payment finished unsuccessfully";
		return PaymentVerificationResult.failure(failureReason);
	}

	private String normalizeLocale(String locale) {
		if ("en".equalsIgnoreCase(locale)) {
			return Locale.EN.getValue();
		}
		return Locale.TR.getValue();
	}

	private Buyer toBuyer(
			String customerId,
			PaymentBuyerRequest buyerRequest,
			PaymentAddressRequest billingAddress,
			String clientIp
	) {
		Buyer buyer = new Buyer();
		buyer.setId(customerId);
		buyer.setName(buyerRequest.name());
		buyer.setSurname(buyerRequest.surname());
		buyer.setGsmNumber(buyerRequest.gsmNumber());
		buyer.setEmail(buyerRequest.email());
		buyer.setIdentityNumber(buyerRequest.identityNumber());
		buyer.setRegistrationAddress(StringUtils.hasText(buyerRequest.registrationAddress())
				? buyerRequest.registrationAddress()
				: billingAddress.address());
		buyer.setIp(StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1");
		buyer.setCity(StringUtils.hasText(buyerRequest.city()) ? buyerRequest.city() : billingAddress.city());
		buyer.setCountry(StringUtils.hasText(buyerRequest.country()) ? buyerRequest.country() : billingAddress.country());
		buyer.setZipCode(StringUtils.hasText(buyerRequest.zipCode()) ? buyerRequest.zipCode() : billingAddress.zipCode());
		buyer.setLastLoginDate("2026-01-01 00:00:00");
		buyer.setRegistrationDate("2026-01-01 00:00:00");
		return buyer;
	}

	private Address toAddress(PaymentAddressRequest addressRequest) {
		Address address = new Address();
		address.setContactName(addressRequest.contactName());
		address.setCity(addressRequest.city());
		address.setCountry(addressRequest.country());
		address.setAddress(addressRequest.address());
		address.setZipCode(addressRequest.zipCode());
		return address;
	}

	private List<BasketItem> toBasketItems(List<OrderItemSummary> items) {
		List<BasketItem> basketItems = new ArrayList<>();
		for (OrderItemSummary item : items) {
			BasketItem basketItem = new BasketItem();
			basketItem.setId(String.valueOf(item.productId()));
			basketItem.setName(item.productName());
			basketItem.setCategory1("General");
			basketItem.setCategory2("E-Commerce");
			basketItem.setItemType(BasketItemType.PHYSICAL.name());
			basketItem.setPrice(item.lineTotal());
			basketItems.add(basketItem);
		}
		return basketItems;
	}

}
