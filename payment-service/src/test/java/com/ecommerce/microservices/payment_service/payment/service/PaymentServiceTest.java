package com.ecommerce.microservices.payment_service.payment.service;

import com.ecommerce.microservices.payment_service.order.client.PaymentOrderClient;
import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;
import com.ecommerce.microservices.payment_service.outbox.service.PaymentOutboxService;
import com.ecommerce.microservices.payment_service.payment.dto.CreatePaymentRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentAddressRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentBuyerRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentCheckoutRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentResponse;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;
import com.ecommerce.microservices.payment_service.payment.exception.DuplicatePendingPaymentException;
import com.ecommerce.microservices.payment_service.payment.gateway.IyzicoPaymentGatewayAdapter;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayAdapter;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayRegistry;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayRequest;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentInitiationResult;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentVerificationResult;
import com.ecommerce.microservices.payment_service.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentOrderClient paymentOrderClient;

	@Mock
	private PaymentGatewayRegistry paymentGatewayRegistry;

	@Mock
	private PaymentGatewayAdapter paymentGatewayAdapter;

	@Mock
	private IyzicoPaymentGatewayAdapter iyzicoPaymentGatewayAdapter;

	@Mock
	private PaymentOutboxService paymentOutboxService;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	void createPaymentInitiatesGatewayAndPersistsPendingPayment() {
		OrderSummary order = new OrderSummary(
				"order-1",
				"customer-1",
				"PENDING_PAYMENT",
				BigDecimal.valueOf(250),
				List.of()
		);
		CreatePaymentRequest request = createPaymentRequest("order-1", PaymentProvider.IYZICO);

		when(paymentOrderClient.getRequiredOrder("order-1", "Bearer token")).thenReturn(order);
		when(paymentRepository.existsByOrderIdAndStatusIn(eq("order-1"), any())).thenReturn(false);
		when(paymentGatewayRegistry.getRequired(PaymentProvider.IYZICO)).thenReturn(paymentGatewayAdapter);
		when(paymentGatewayAdapter.initiate(any(PaymentGatewayRequest.class)))
				.thenReturn(new PaymentInitiationResult("ext-1", "https://checkout.example.com/ext-1"));
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PaymentResponse response = paymentService.createPayment(
				"customer-1",
				"Bearer token",
				request,
				"127.0.0.1"
		);

		assertThat(response.orderId()).isEqualTo("order-1");
		assertThat(response.customerId()).isEqualTo("customer-1");
		assertThat(response.provider()).isEqualTo(PaymentProvider.IYZICO);
		assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
		assertThat(response.amount()).isEqualByComparingTo("250");
		assertThat(response.externalPaymentId()).isEqualTo("ext-1");
		assertThat(response.checkoutUrl()).isEqualTo("https://checkout.example.com/ext-1");

		ArgumentCaptor<PaymentGatewayRequest> gatewayRequestCaptor = ArgumentCaptor.forClass(PaymentGatewayRequest.class);
		verify(paymentGatewayAdapter).initiate(gatewayRequestCaptor.capture());
		assertThat(gatewayRequestCaptor.getValue().order().id()).isEqualTo("order-1");
		assertThat(gatewayRequestCaptor.getValue().customerId()).isEqualTo("customer-1");
		assertThat(gatewayRequestCaptor.getValue().clientIp()).isEqualTo("127.0.0.1");
	}

	@Test
	void createPaymentRejectsDuplicatePendingOrSuccessfulPayments() {
		OrderSummary order = new OrderSummary(
				"order-1",
				"customer-1",
				"PENDING_PAYMENT",
				BigDecimal.valueOf(250),
				List.of()
		);

		when(paymentOrderClient.getRequiredOrder("order-1", "Bearer token")).thenReturn(order);
		when(paymentRepository.existsByOrderIdAndStatusIn(eq("order-1"), any())).thenReturn(true);

		assertThatThrownBy(() -> paymentService.createPayment(
				"customer-1",
				"Bearer token",
				createPaymentRequest("order-1", PaymentProvider.IYZICO),
				"127.0.0.1"
		))
				.isInstanceOf(DuplicatePendingPaymentException.class);

		verify(paymentGatewayRegistry, never()).getRequired(any());
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void handleIyzicoCallbackMarksPaymentSucceededAndPublishesOutboxEvent() {
		PaymentEntity payment = new PaymentEntity(
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				BigDecimal.valueOf(250),
				"token-1",
				"https://checkout.example.com/token-1",
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233"
		);

		when(paymentRepository.findByExternalPaymentIdAndProvider("token-1", PaymentProvider.IYZICO))
				.thenReturn(Optional.of(payment));
		when(iyzicoPaymentGatewayAdapter.retrieve("token-1")).thenReturn(PaymentVerificationResult.success());
		when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PaymentResponse response = paymentService.handleIyzicoCallback("token-1");

		assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(response.completedAt()).isNotNull();
		verify(iyzicoPaymentGatewayAdapter).retrieve("token-1");
		verify(paymentOutboxService).appendSucceededEvent(payment);
		verify(paymentRepository).save(payment);
	}

	private CreatePaymentRequest createPaymentRequest(String orderId, PaymentProvider provider) {
		PaymentBuyerRequest buyer = new PaymentBuyerRequest(
				"Onur",
				"Sevinc",
				"onur@example.com",
				"905551112233",
				"11111111110",
				"Maslak Mahallesi Buyukdere Caddesi No:1",
				"Istanbul",
				"Turkiye",
				"34000"
		);
		PaymentAddressRequest address = new PaymentAddressRequest(
				"Onur Sevinc",
				"Maslak Mahallesi Buyukdere Caddesi No:1",
				"Istanbul",
				"Turkiye",
				"34000"
		);

		return new CreatePaymentRequest(
				orderId,
				provider,
				new PaymentCheckoutRequest("tr", buyer, address, address)
		);
	}
}
