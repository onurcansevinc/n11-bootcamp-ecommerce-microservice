package com.ecommerce.microservices.payment_service.payment.controller;

import com.ecommerce.microservices.common.web.response.ApiResponse;
import com.ecommerce.microservices.common.web.response.ResponseMeta;
import com.ecommerce.microservices.payment_service.payment.dto.CreatePaymentRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentResponse;
import com.ecommerce.microservices.payment_service.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Tag(name = "Payments", description = "Payment initiation and sandbox simulation endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;
	private final String paymentFrontendResultUrl;

	public PaymentController(
			PaymentService paymentService,
			@Value("${payment.frontend-result-url:http://localhost:5173/payment/result}") String paymentFrontendResultUrl
	) {
		this.paymentService = paymentService;
		this.paymentFrontendResultUrl = paymentFrontendResultUrl;
	}

	@PostMapping
	@Operation(summary = "Create payment for an order")
	public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
			@Valid @RequestBody CreatePaymentRequest request,
			JwtAuthenticationToken authentication,
			HttpServletRequest httpServletRequest
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"Payment created successfully",
						paymentService.createPayment(
								currentCustomerId(authentication),
								bearerToken(authentication),
								request,
								httpServletRequest.getRemoteAddr()
						)
				));
	}

	@GetMapping("/{paymentId}")
	@Operation(summary = "Get payment by id")
	public ApiResponse<PaymentResponse> getPaymentById(
			@PathVariable String paymentId,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Payment fetched successfully",
				paymentService.getPaymentById(paymentId, currentCustomerId(authentication))
		);
	}

	@GetMapping
	@Operation(summary = "List current user's payments")
	public ApiResponse<List<PaymentResponse>> getPayments(
			@RequestParam(defaultValue = "0") @Min(0) Integer page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
			JwtAuthenticationToken authentication
	) {
		Page<PaymentResponse> payments = paymentService.getPayments(currentCustomerId(authentication), page, size);
		return ApiResponse.success(
				"Payments fetched successfully",
				payments.getContent(),
				ResponseMeta.from(payments)
		);
	}

	@PostMapping("/{paymentId}/simulate-success")
	@Operation(summary = "Simulate successful sandbox payment")
	public ApiResponse<PaymentResponse> simulateSuccess(
			@PathVariable String paymentId,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Payment marked as successful",
				paymentService.simulateSuccess(paymentId, currentCustomerId(authentication))
		);
	}

	@PostMapping("/{paymentId}/simulate-failure")
	@Operation(summary = "Simulate failed sandbox payment")
	public ApiResponse<PaymentResponse> simulateFailure(
			@PathVariable String paymentId,
			JwtAuthenticationToken authentication
	) {
		return ApiResponse.success(
				"Payment marked as failed",
				paymentService.simulateFailure(paymentId, currentCustomerId(authentication))
		);
	}

	@PostMapping("/iyzico/callback")
	@Operation(summary = "Handle Iyzico checkout callback")
	public ResponseEntity<Void> handleIyzicoCallback(@RequestParam String token) {
		PaymentResponse payment = paymentService.handleIyzicoCallback(token);
		String redirectUrl = UriComponentsBuilder.fromUriString(paymentFrontendResultUrl)
				.queryParam("paymentId", payment.id())
				.queryParam("orderId", payment.orderId())
				.queryParam("status", payment.status())
				.build()
				.toUriString();

		return ResponseEntity.status(HttpStatus.FOUND)
				header(HttpHeaders.LOCATION, redirectUrl)
				.build();
	}

	private String currentCustomerId(JwtAuthenticationToken authentication) {
		return authentication.getToken().getSubject();
	}

	private String bearerToken(JwtAuthenticationToken authentication) {
		return authentication.getToken().getTokenValue();
	}

}
