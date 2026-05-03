package com.ecommerce.microservices.payment_service.payment.controller;

import com.ecommerce.microservices.common.web.security.CurrentCustomerIdResolver;
import com.ecommerce.microservices.payment_service.common.exception.GlobalExceptionHandler;
import com.ecommerce.microservices.payment_service.payment.dto.CreatePaymentRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentAddressRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentBuyerRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentCheckoutRequest;
import com.ecommerce.microservices.payment_service.payment.dto.PaymentResponse;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentProvider;
import com.ecommerce.microservices.payment_service.payment.entity.PaymentStatus;
import com.ecommerce.microservices.payment_service.payment.exception.DuplicatePendingPaymentException;
import com.ecommerce.microservices.payment_service.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = PaymentController.class,
		excludeAutoConfiguration = {
				SecurityAutoConfiguration.class,
				OAuth2ResourceServerAutoConfiguration.class
		},
		properties = {
				"spring.cloud.config.enabled=false",
				"spring.cloud.config.import-check.enabled=false",
				"spring.cloud.discovery.enabled=false",
				"eureka.client.enabled=false"
		}
)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "payment.frontend-result-url=http://localhost:5173/payment/result")
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PaymentService paymentService;

	@MockBean
	private CurrentCustomerIdResolver currentCustomerIdResolver;

	@Test
	void createPaymentReturnsCreatedResponseAndDelegatesToService() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		CreatePaymentRequest request = createPaymentRequest("order-1");
		PaymentResponse paymentResponse = new PaymentResponse(
				"payment-1",
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				PaymentStatus.PENDING,
				BigDecimal.valueOf(250),
				"ext-1",
				"https://checkout.example.com/ext-1",
				null,
				null,
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(paymentService.createPayment(eq("customer-1"), eq("user-token"), eq(request), eq("127.0.0.1")))
				.thenReturn(paymentResponse);

		mockMvc.perform(post("/api/v1/payments")
						.principal(authentication)
						.with(requestBuilder -> {
							requestBuilder.setRemoteAddr("127.0.0.1");
							return requestBuilder;
						})
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Payment created successfully"))
				.andExpect(jsonPath("$.data.id").value("payment-1"))
				.andExpect(jsonPath("$.data.orderId").value("order-1"))
				.andExpect(jsonPath("$.data.customerId").value("customer-1"))
				.andExpect(jsonPath("$.data.provider").value("IYZICO"))
				.andExpect(jsonPath("$.data.status").value("PENDING"));

		verify(paymentService).createPayment("customer-1", "user-token", request, "127.0.0.1");
	}

	@Test
	void createPaymentReturnsBadRequestForInvalidBody() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");

		mockMvc.perform(post("/api/v1/payments")
						.principal(authentication)
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "orderId": "",
								  "provider": "IYZICO",
								  "checkout": null
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.detail").value("Request body validation failed"));

		verify(paymentService, never()).createPayment(any(), any(), any(), any());
	}

	@Test
	void createPaymentMapsBusinessConflictToProblemDetail() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		CreatePaymentRequest request = createPaymentRequest("order-1");

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(paymentService.createPayment(eq("customer-1"), eq("user-token"), eq(request), eq("127.0.0.1")))
				.thenThrow(new DuplicatePendingPaymentException("order-1"));

		mockMvc.perform(post("/api/v1/payments")
						.principal(authentication)
						.with(requestBuilder -> {
							requestBuilder.setRemoteAddr("127.0.0.1");
							return requestBuilder;
						})
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Payment request conflict"))
				.andExpect(jsonPath("$.detail").value("Order order-1 already has an active or successful payment"));
	}

	@Test
	void getPaymentsReturnsPaginatedResponseMeta() throws Exception {
		JwtAuthenticationToken authentication = jwtAuthenticationToken("user-token");
		PaymentResponse paymentResponse = new PaymentResponse(
				"payment-1",
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				PaymentStatus.SUCCESS,
				BigDecimal.valueOf(250),
				"ext-1",
				"https://checkout.example.com/ext-1",
				null,
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		when(currentCustomerIdResolver.resolve(authentication)).thenReturn("customer-1");
		when(paymentService.getPayments("customer-1", 0, 5))
				.thenReturn(new PageImpl<>(List.of(paymentResponse), PageRequest.of(0, 5), 1));

		mockMvc.perform(get("/api/v1/payments")
						.principal(authentication)
						.param("page", "0")
						.param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Payments fetched successfully"))
				.andExpect(jsonPath("$.data[0].id").value("payment-1"))
				.andExpect(jsonPath("$.meta.page").value(0))
				.andExpect(jsonPath("$.meta.size").value(5))
				.andExpect(jsonPath("$.meta.totalElements").value(1));
	}

	@Test
	void handleIyzicoCallbackRedirectsToFrontendResultPage() throws Exception {
		PaymentResponse paymentResponse = new PaymentResponse(
				"payment-1",
				"order-1",
				"customer-1",
				PaymentProvider.IYZICO,
				PaymentStatus.SUCCESS,
				BigDecimal.valueOf(250),
				"ext-1",
				"https://checkout.example.com/ext-1",
				null,
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		when(paymentService.handleIyzicoCallback("token-1")).thenReturn(paymentResponse);

		mockMvc.perform(post("/api/v1/payments/iyzico/callback").param("token", "token-1"))
				.andExpect(status().isFound())
				.andExpect(header().string(
						"Location",
						"http://localhost:5173/payment/result?paymentId=payment-1&orderId=order-1&status=SUCCESS"
				));
	}

	private JwtAuthenticationToken jwtAuthenticationToken(String tokenValue) {
		Jwt jwt = Jwt.withTokenValue(tokenValue)
				.header("alg", "none")
				.subject("customer-1")
				.claim("preferred_username", "customer-user")
				.build();
		return new JwtAuthenticationToken(jwt);
	}

	private CreatePaymentRequest createPaymentRequest(String orderId) {
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
				PaymentProvider.IYZICO,
				new PaymentCheckoutRequest("tr", buyer, address, address)
		);
	}
}
