package com.ecommerce.microservices.payment_service.payment.service;

import com.ecommerce.microservices.payment_service.order.client.PaymentOrderClient;
import com.ecommerce.microservices.payment_service.order.dto.OrderSummary;
import com.ecommerce.microservices.payment_service.payment.dto.CreatePaymentRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentResponse;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentEntity;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;
import com.ecommerce.microservices.payment_service.payment.exception.DuplicatePendingPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.InvalidPaymentStateException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderNotPayableException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentAccessDeniedException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentNotFoundException;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayAdapter;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayRequest;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentGatewayRegistry;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentInitiationResult;
import com.ecommerce.microservices.payment_service.payment.gateway.PaymentVerificationResult;
import com.ecommerce.microservices.payment_service.payment.gateway.IyzicoPaymentGatewayAdapter;
import com.ecommerce.microservices.payment_service.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentOrderClient paymentOrderClient;
	private final PaymentGatewayRegistry paymentGatewayRegistry;
	private final IyzicoPaymentGatewayAdapter iyzicoPaymentGatewayAdapter;

	public PaymentService(
			PaymentRepository paymentRepository,
			PaymentOrderClient paymentOrderClient,
			PaymentGatewayRegistry paymentGatewayRegistry,
			IyzicoPaymentGatewayAdapter iyzicoPaymentGatewayAdapter
	) {
		this.paymentRepository = paymentRepository;
		this.paymentOrderClient = paymentOrderClient;
		this.paymentGatewayRegistry = paymentGatewayRegistry;
		this.iyzicoPaymentGatewayAdapter = iyzicoPaymentGatewayAdapter;
	}

	@Transactional
	public PaymentResponse createPayment(String customerId, String bearerToken, CreatePaymentRequest request, String clientIp) {
		OrderSummary order = paymentOrderClient.getRequiredOrder(request.orderId(), bearerToken);
		assertOrderPayable(order);

		if (paymentRepository.existsByOrderIdAndStatusIn(
				order.id(),
				List.of(PaymentStatus.PENDING, PaymentStatus.SUCCESS)
		)) {
			throw new DuplicatePendingPaymentException(order.id());
		}

		PaymentGatewayAdapter paymentGatewayAdapter = paymentGatewayRegistry.getRequired(request.provider());
		PaymentInitiationResult initiationResult = paymentGatewayAdapter.initiate(
				new PaymentGatewayRequest(order, customerId, request.checkout(), clientIp)
		);

		PaymentEntity persistedPayment = paymentRepository.save(new PaymentEntity(
				order.id(),
				customerId,
				request.provider(),
				order.totalAmount(),
				initiationResult.externalPaymentId(),
				initiationResult.checkoutUrl()
		));

		return PaymentResponse.from(persistedPayment);
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPaymentById(String paymentId, String customerId) {
		return PaymentResponse.from(getOwnedPayment(paymentId, customerId));
	}

	@Transactional(readOnly = true)
	public Page<PaymentResponse> getPayments(String customerId, int page, int size) {
		return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(
						customerId,
						PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
				)
				.map(PaymentResponse::from);
	}

	@Transactional
	public PaymentResponse simulateSuccess(String paymentId, String customerId) {
		PaymentEntity payment = getOwnedPayment(paymentId, customerId);
		assertPending(payment);

		paymentOrderClient.markPaymentSucceeded(payment.getOrderId());
		payment.markSucceeded();

		return PaymentResponse.from(paymentRepository.save(payment));
	}

	@Transactional
	public PaymentResponse simulateFailure(String paymentId, String customerId) {
		PaymentEntity payment = getOwnedPayment(paymentId, customerId);
		assertPending(payment);

		paymentOrderClient.markPaymentFailed(payment.getOrderId());
		payment.markFailed("Sandbox payment marked as failed");

		return PaymentResponse.from(paymentRepository.save(payment));
	}

	@Transactional
	public PaymentResponse handleIyzicoCallback(String token) {
		PaymentEntity payment = paymentRepository.findByExternalPaymentIdAndProvider(token, PaymentProvider.IYZICO)
				.orElseThrow(() -> new PaymentNotFoundException(token));

		if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED) {
			return PaymentResponse.from(payment);
		}

		PaymentVerificationResult verificationResult = iyzicoPaymentGatewayAdapter.retrieve(token);
		if (verificationResult.successful()) {
			paymentOrderClient.markPaymentSucceeded(payment.getOrderId());
			payment.markSucceeded();
		} else {
			paymentOrderClient.markPaymentFailed(payment.getOrderId());
			payment.markFailed(verificationResult.failureReason());
		}

		return PaymentResponse.from(paymentRepository.save(payment));
	}

	private PaymentEntity getOwnedPayment(String paymentId, String customerId) {
		PaymentEntity payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(paymentId));

		if (!payment.getCustomerId().equals(customerId)) {
			throw new PaymentAccessDeniedException(paymentId);
		}

		return payment;
	}

	private void assertOrderPayable(OrderSummary order) {
		if (!"PENDING_PAYMENT".equals(order.status())) {
			throw new OrderNotPayableException(order.id(), order.status());
		}
	}

	private void assertPending(PaymentEntity payment) {
		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new InvalidPaymentStateException(
					payment.getId(),
					PaymentStatus.PENDING.name(),
					payment.getStatus().name()
			);
		}
	}

}
